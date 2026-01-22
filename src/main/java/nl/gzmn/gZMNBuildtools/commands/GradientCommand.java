package nl.gzmn.gZMNBuildtools.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.gradient.*;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;
import nl.gzmn.gZMNBuildtools.util.MessageManager;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.InterpolationMode;
import nl.gzmn.gZMNBuildtools.ui.GradientUIManager;
import org.bukkit.entity.Player;

import java.util.*;

public class GradientCommand {

    private final GradientUIManager uiManager;
    private GradientStorageManager storageManager;

    // Track last used gradient per player for quick saving
    private final Map<UUID, LastGradientInfo> lastUsedGradients = new HashMap<>();

    private static final List<String> BLOCK_SUGGESTIONS = Arrays.asList(
            "stone,cobblestone,andesite",
            "white_wool,light_gray_wool,gray_wool,black_wool",
            "oak_planks,spruce_planks,dark_oak_planks",
            "sandstone,red_sandstone",
            "grass_block,dirt,coarse_dirt");

    private static final List<String> DIRECTION_SUGGESTIONS = Arrays.asList(
            "VERTICAL_UP", "VERTICAL_DOWN", "HORIZONTAL_X", "HORIZONTAL_Z", "RADIAL");

    private static final List<String> MODE_SUGGESTIONS = Arrays.asList(
            "LINEAR", "SMOOTH", "DISCRETE", "BLENDED", "NOISE");

    public GradientCommand(GradientUIManager uiManager) {
        this.uiManager = uiManager;
    }

    public void setStorageManager(GradientStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    /**
     * Get preset ID suggestions for tab completion.
     */
    public List<String> getPresetSuggestions() {
        return GradientPresets.getAllIds();
    }

    public List<String> getBlockSuggestions() {
        return BLOCK_SUGGESTIONS;
    }

    public List<String> getDirectionSuggestions() {
        return DIRECTION_SUGGESTIONS;
    }

    public List<String> getModeSuggestions() {
        return MODE_SUGGESTIONS;
    }

    public void openUI(Player player) {
        if (uiManager != null) {
            uiManager.openGradientUI(player);
        } else {
            MessageManager.error(player, "Gradient UI is not available.");
        }
    }

    /**
     * Open the easy mode UI directly.
     */
    public void openEasyMode(Player player) {
        if (uiManager != null) {
            uiManager.openEasyMode(player);
        } else {
            MessageManager.error(player, "Gradient UI is not available.");
        }
    }

    /**
     * Open the advanced mode UI directly.
     */
    public void openAdvancedMode(Player player) {
        if (uiManager != null) {
            uiManager.openAdvancedMode(player);
        } else {
            MessageManager.error(player, "Gradient UI is not available.");
        }
    }

    /**
     * Apply a preset gradient directly via command.
     */
    public void executePreset(Player player, String presetId, String directionString) {
        try {
            GradientPreset preset = GradientPresets.get(presetId);
            if (preset == null) {
                MessageManager.error(player, "Unknown preset: %s", presetId);
                MessageManager.send(player, Component.text("Available presets: " +
                        String.join(", ", GradientPresets.getAllIds()), NamedTextColor.GRAY));
                return;
            }

            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            MessageManager.info(player, "Applying preset: %s", preset.getDisplayName());

            GradientExecutor.GradientResult result = GradientExecutor.getInstance().applyPreset(
                    actor, region, preset, direction, false, null);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Invalid direction: %s", directionString);
            MessageManager.send(player, Component.text("Valid directions: " +
                    String.join(", ", DIRECTION_SUGGESTIONS), NamedTextColor.GRAY));
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute a noise gradient via command.
     */
    public void executeNoise(Player player, String blocksString, String directionString,
                              double scale, double strength) {
        try {
            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            GradientDefinition gradient = GradientDefinition.parse(blocksString, direction, InterpolationMode.LINEAR);
            NoiseSettings noiseSettings = NoiseSettings.builder()
                    .scale(scale)
                    .strength(strength)
                    .build();

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            MessageManager.info(player, "Applying noise gradient (scale=%.2f, strength=%.2f)", scale, strength);

            GradientExecutor.GradientResult result = GradientExecutor.getInstance().applyNoise(
                    actor, region, gradient, noiseSettings);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Error: %s", e.getMessage());
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    public void execute(Player player, String blocksString, String directionString, String modeString) {
        try {
            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            InterpolationMode mode = InterpolationMode.valueOf(modeString.toUpperCase());

            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            GradientDefinition gradient = GradientDefinition.parse(blocksString, direction, mode);

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);

            MessageManager.info(player, "Applying %s", "gradient");

            int affected = applyGradient(actor, region, gradient);

            MessageManager.success(player, "Gradient applied to %d blocks.", affected);

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Error: %s", e.getMessage());
            MessageManager.send(player,
                    Component.text("Valid directions: VERTICAL_UP, VERTICAL_DOWN, HORIZONTAL_X, HORIZONTAL_Z, RADIAL",
                            NamedTextColor.GRAY));
            MessageManager.send(player, Component.text("Valid modes: LINEAR, SMOOTH, DISCRETE, BLENDED", NamedTextColor.GRAY));
        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    public int applyGradient(com.sk89q.worldedit.entity.Player actor, Region region, GradientDefinition gradient) {
        int count = 0;

        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        // Support for BLENDED mode
        boolean useBlended = gradient.getInterpolationMode() == InterpolationMode.BLENDED;
        long seed = System.currentTimeMillis();

        try (EditSession editSession = localSession.createEditSession(actor)) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            double minValue, maxValue;
            switch (gradient.getDirection()) {
                case VERTICAL_UP:
                case VERTICAL_DOWN:
                    minValue = min.y();
                    maxValue = max.y();
                    break;
                case HORIZONTAL_X:
                    minValue = min.x();
                    maxValue = max.x();
                    break;
                case HORIZONTAL_Z:
                    minValue = min.z();
                    maxValue = max.z();
                    break;
                case RADIAL:
                    BlockVector3 center = region.getCenter().toBlockPoint();
                    minValue = 0;
                    maxValue = Math.max(
                            Math.max(Math.abs(max.x() - center.x()), Math.abs(min.x() - center.x())),
                            Math.max(Math.abs(max.z() - center.z()), Math.abs(min.z() - center.z())));
                    break;
                default:
                    minValue = 0;
                    maxValue = 1;
            }

            double range = maxValue - minValue;
            if (range == 0)
                range = 1;

            for (BlockVector3 position : region) {
                double value;

                switch (gradient.getDirection()) {
                    case VERTICAL_UP:
                        value = position.y();
                        break;
                    case VERTICAL_DOWN:
                        value = maxValue - (position.y() - minValue);
                        break;
                    case HORIZONTAL_X:
                        value = position.x();
                        break;
                    case HORIZONTAL_Z:
                        value = position.z();
                        break;
                    case RADIAL:
                        BlockVector3 center = region.getCenter().toBlockPoint();
                        double dx = position.x() - center.x();
                        double dz = position.z() - center.z();
                        value = Math.sqrt(dx * dx + dz * dz);
                        break;
                    default:
                        value = minValue;
                }

                double normalizedPosition = (value - minValue) / range;

                BlockType blockType;
                if (useBlended) {
                    // Position-based seed for deterministic per-block results
                    long posHash = seed ^ (position.x() * 73856093L) ^ (position.y() * 19349663L) ^ (position.z() * 83492791L);
                    Random random = new Random(posHash);
                    blockType = gradient.getBlockAt(normalizedPosition, random);
                } else {
                    blockType = gradient.getBlockAt(normalizedPosition);
                }

                if (blockType != null) {
                    boolean placed = editSession.setBlock(position, blockType.getDefaultState());
                    count++;
                }
            }

            localSession.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    protected Region getSelectionFromPlayer(org.bukkit.entity.Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
        } catch (Exception ex) {
            return null;
        }
    }

    // ===== Saved Gradient Commands =====

    /**
     * Save the last used gradient or a new gradient definition.
     */
    public void saveGradient(Player player, String name, boolean isPublic) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        LastGradientInfo lastInfo = lastUsedGradients.get(player.getUniqueId());
        if (lastInfo == null) {
            MessageManager.error(player, "No recent gradient to save. Use a gradient first, or specify blocks.");
            MessageManager.info(player, "Usage: /gradient save <name> <blocks> <direction> [mode]");
            return;
        }

        String id = "saved_" + UUID.randomUUID().toString().substring(0, 8);

        GradientPreset preset = new GradientPreset.Builder(id)
                .displayName(name)
                .description("Saved by " + player.getName())
                .icon(org.bukkit.Material.CHEST)
                .category(GradientPreset.PresetCategory.CUSTOM)
                .blocks(lastInfo.blockIds)
                .build();

        SavedGradient saved = new SavedGradient(
                id, name, player.getUniqueId(), player.getName(),
                preset, isPublic, System.currentTimeMillis()
        );

        storageManager.saveGradient(saved);
        MessageManager.success(player, "Gradient '%s' saved%s!", name, isPublic ? " (public)" : "");
    }

    /**
     * Save a gradient with explicit block definition.
     */
    public void saveGradientWithBlocks(Player player, String name, String blocksString,
                                        String directionString, String modeString, boolean isPublic) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        try {
            // Validate the gradient definition
            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            InterpolationMode mode = InterpolationMode.valueOf(modeString.toUpperCase());
            GradientDefinition.parse(blocksString, direction, mode); // Validate it parses

            String id = "saved_" + UUID.randomUUID().toString().substring(0, 8);

            // Convert blocks string to list format
            List<String> blockIds = Arrays.asList(blocksString.split(","));

            GradientPreset preset = new GradientPreset.Builder(id)
                    .displayName(name)
                    .description("Saved by " + player.getName())
                    .icon(org.bukkit.Material.CHEST)
                    .category(GradientPreset.PresetCategory.CUSTOM)
                    .blocks(blockIds)
                    .build();

            SavedGradient saved = new SavedGradient(
                    id, name, player.getUniqueId(), player.getName(),
                    preset, isPublic, System.currentTimeMillis()
            );

            storageManager.saveGradient(saved);
            MessageManager.success(player, "Gradient '%s' saved%s!", name, isPublic ? " (public)" : "");

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Invalid gradient: %s", e.getMessage());
        }
    }

    /**
     * Use a saved gradient.
     */
    public void useGradient(Player player, String name, String directionString) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        Optional<SavedGradient> found = storageManager.findByName(player.getUniqueId(), name);
        if (found.isEmpty()) {
            MessageManager.error(player, "Gradient '%s' not found.", name);
            MessageManager.info(player, "Use /gradient list to see available gradients.");
            return;
        }

        SavedGradient saved = found.get();
        GradientPreset preset = saved.getPreset();

        try {
            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            MessageManager.info(player, "Applying saved gradient: %s", saved.getName());

            GradientExecutor.GradientResult result = GradientExecutor.getInstance().applyPreset(
                    actor, region, preset, direction, false, null);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());

                // Track as last used
                lastUsedGradients.put(player.getUniqueId(), new LastGradientInfo(
                        preset.getBlockIds(), direction.name(), "LINEAR"
                ));
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Invalid direction: %s", directionString);
            MessageManager.send(player, Component.text("Valid directions: " +
                    String.join(", ", DIRECTION_SUGGESTIONS), NamedTextColor.GRAY));
        }
    }

    /**
     * List saved gradients.
     */
    public void listGradients(Player player, boolean showGlobal) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        if (showGlobal) {
            List<SavedGradient> globals = storageManager.getGlobalGradients();
            if (globals.isEmpty()) {
                MessageManager.info(player, "No public gradients available.");
                return;
            }

            MessageManager.info(player, "Public Gradients (%d):", globals.size());
            for (SavedGradient sg : globals) {
                MessageManager.send(player, Component.text("  • " + sg.getName())
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(" by " + sg.getAuthorName()).color(NamedTextColor.GRAY)));
            }
        } else {
            List<SavedGradient> playerGradients = storageManager.getPlayerGradients(player.getUniqueId());
            if (playerGradients.isEmpty()) {
                MessageManager.info(player, "You have no saved gradients.");
                MessageManager.info(player, "Use /gradient save <name> after applying a gradient.");
                return;
            }

            MessageManager.info(player, "Your Gradients (%d):", playerGradients.size());
            for (SavedGradient sg : playerGradients) {
                String suffix = sg.isPublic() ? " (public)" : "";
                MessageManager.send(player, Component.text("  • " + sg.getName() + suffix)
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Delete a saved gradient.
     */
    public void deleteGradient(Player player, String name) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        Optional<SavedGradient> found = storageManager.findByName(player.getUniqueId(), name);
        if (found.isEmpty()) {
            MessageManager.error(player, "Gradient '%s' not found.", name);
            return;
        }

        SavedGradient saved = found.get();

        // Only allow deleting own gradients
        if (!saved.getAuthorInfo().equals(player.getUniqueId())) {
            MessageManager.error(player, "You can only delete your own gradients.");
            return;
        }

        storageManager.deleteGradient(player.getUniqueId(), saved.getId());
        MessageManager.success(player, "Gradient '%s' deleted.", name);
    }

    /**
     * Toggle public visibility of a saved gradient.
     */
    public void shareGradient(Player player, String name) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        Optional<SavedGradient> found = storageManager.findByName(player.getUniqueId(), name);
        if (found.isEmpty()) {
            MessageManager.error(player, "Gradient '%s' not found.", name);
            return;
        }

        SavedGradient saved = found.get();

        if (!saved.getAuthorInfo().equals(player.getUniqueId())) {
            MessageManager.error(player, "You can only share your own gradients.");
            return;
        }

        storageManager.togglePublic(player.getUniqueId(), saved.getId());
        boolean nowPublic = !saved.isPublic();
        MessageManager.success(player, "Gradient '%s' is now %s.", name, nowPublic ? "public" : "private");
    }

    /**
     * Get suggestions for saved gradient names (for tab completion).
     */
    public List<String> getSavedGradientSuggestions(Player player) {
        if (storageManager == null) {
            return Collections.emptyList();
        }
        return storageManager.getPlayerGradientNames(player.getUniqueId());
    }

    /**
     * Track a gradient execution for later saving.
     */
    public void trackLastGradient(UUID playerId, List<String> blockIds, String direction, String mode) {
        lastUsedGradients.put(playerId, new LastGradientInfo(blockIds, direction, mode));
    }

    /**
     * Info about the last gradient a player used.
     */
    private static class LastGradientInfo {
        final List<String> blockIds;
        final String direction;
        final String mode;

        LastGradientInfo(List<String> blockIds, String direction, String mode) {
            this.blockIds = blockIds;
            this.direction = direction;
            this.mode = mode;
        }
    }
}
