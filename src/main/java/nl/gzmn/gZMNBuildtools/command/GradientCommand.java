package nl.gzmn.gZMNBuildtools.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.config.GradientPresets;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition.InterpolationMode;
import nl.gzmn.gZMNBuildtools.gradient.service.GradientExecutor;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GradientCommand {

    private static final Logger LOGGER = Logger.getLogger("GZMNBuildtools");

    private GradientStorageManager storageManager;
    private GradientExecutor executor = new GradientExecutor();
    private final Map<UUID, LastGradientInfo> lastUsedGradients = new HashMap<>();

    private static final List<String> DIRECTION_SUGGESTIONS = Arrays.asList(
            "up", "down", "x", "z", "radial", // Short aliases
            "VERTICAL_UP", "VERTICAL_DOWN", "HORIZONTAL_X", "HORIZONTAL_Z", "RADIAL");

    private static final List<String> MODE_SUGGESTIONS = Arrays.stream(InterpolationMode.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    // Direction aliases mapping
    private static final Map<String, GradientDirection> DIRECTION_ALIASES = new HashMap<>();

    static {
        DIRECTION_ALIASES.put("up", GradientDirection.VERTICAL_UP);
        DIRECTION_ALIASES.put("down", GradientDirection.VERTICAL_DOWN);
        DIRECTION_ALIASES.put("x", GradientDirection.HORIZONTAL_X);
        DIRECTION_ALIASES.put("z", GradientDirection.HORIZONTAL_Z);
        DIRECTION_ALIASES.put("radial", GradientDirection.RADIAL);
    }

    /**
     * Parse direction string with support for short aliases.
     */
    public static GradientDirection parseDirection(String directionString) {
        String lower = directionString.toLowerCase();
        if (DIRECTION_ALIASES.containsKey(lower)) {
            return DIRECTION_ALIASES.get(lower);
        }
        return GradientDirection.valueOf(directionString.toUpperCase());
    }

    // No hardcoded block suggestions - let users type their own blocks

    public GradientCommand() {
    }

    public void setStorageManager(GradientStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void setExecutor(GradientExecutor executor) {
        this.executor = executor;
    }

    public List<String> getDirectionSuggestions() {
        return DIRECTION_SUGGESTIONS;
    }

    public List<String> getModeSuggestions() {
        return MODE_SUGGESTIONS;
    }

    public List<String> getBlockSuggestions() {
        return com.sk89q.worldedit.world.block.BlockType.REGISTRY
                .values().stream()
                .map(bt -> bt.toString().replace("minecraft:", ""))
                .sorted()
                .limit(50)
                .map(id -> "[" + id + "]")
                .collect(Collectors.toList());
    }

    public List<String> getPresetSuggestions() {
        return GradientPresets.getAllIds();
    }

    public void executePreset(Player player, String presetId, String directionString) {
        try {
            GradientPreset preset = GradientPresets.get(presetId);
            if (preset == null) {
                MessageManager.error(player, "Unknown preset: %s", presetId);
                MessageManager.send(player, Component.text("Available presets: " +
                        String.join(", ", GradientPresets.getAllIds()), NamedTextColor.GRAY));
                return;
            }

            GradientDirection direction = parseDirection(directionString);
            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            MessageManager.info(player, "Applying preset: %s", preset.getDisplayName());

            GradientExecutor.GradientResult result = executor.applyPreset(
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
            LOGGER.log(Level.SEVERE, "Unexpected error in gradient command", e);
        }
    }

    public void executeNoise(Player player, String blocksString, String directionString,
                             double scale, double strength) {
        try {
            GradientDirection direction = parseDirection(directionString);
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

            GradientExecutor.GradientResult result = executor.applyNoise(
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
            LOGGER.log(Level.SEVERE, "Unexpected error in gradient command", e);
        }
    }

    public void execute(Player player, String blocksString, String directionString, String modeString) {
        try {
            GradientDirection direction = parseDirection(directionString);
            InterpolationMode mode = InterpolationMode.valueOf(modeString.toUpperCase());

            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            GradientDefinition gradient = GradientDefinition.parse(blocksString, direction, mode);

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);

            MessageManager.info(player, "Applying %s", "gradient");

            GradientExecutor.GradientResult result = executor.applyLinear(actor, region,
                    gradient);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
                trackLastGradient(player.getUniqueId(), gradient.getBlockIds(), directionString, modeString);
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Error: %s", e.getMessage());
            MessageManager.send(player,
                    Component.text("Valid directions: " + String.join(", ", DIRECTION_SUGGESTIONS),
                            NamedTextColor.GRAY));
            MessageManager.send(player,
                    Component.text("Valid modes: " + String.join(", ", MODE_SUGGESTIONS), NamedTextColor.GRAY));
        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            LOGGER.log(Level.SEVERE, "Unexpected error in gradient command", e);
        }
    }

    protected Region getSelectionFromPlayer(org.bukkit.entity.Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
        } catch (Exception ex) {
            return null;
        }
    }

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
                preset, isPublic, System.currentTimeMillis());

        storageManager.saveGradient(saved);
        MessageManager.success(player, "Gradient '%s' saved%s!", name, isPublic ? " (public)" : "");
    }

    public void saveGradientWithBlocks(Player player, String name, String blocksString,
                                       String directionString, String modeString, boolean isPublic) {
        if (storageManager == null) {
            MessageManager.error(player, "Storage manager not available.");
            return;
        }

        try {
            GradientDirection direction = parseDirection(directionString);
            InterpolationMode mode = InterpolationMode.valueOf(modeString.toUpperCase());
            GradientDefinition.parse(blocksString, direction, mode);

            String id = "saved_" + UUID.randomUUID().toString().substring(0, 8);
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
                    preset, isPublic, System.currentTimeMillis());

            storageManager.saveGradient(saved);
            MessageManager.success(player, "Gradient '%s' saved%s!", name, isPublic ? " (public)" : "");

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Invalid gradient: %s", e.getMessage());
        }
    }

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
            GradientDirection direction = parseDirection(directionString);
            Region region = getSelectionFromPlayer(player);

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            MessageManager.info(player, "Applying saved gradient: %s", saved.getName());

            GradientExecutor.GradientResult result = executor.applyPreset(
                    actor, region, preset, direction, false, null);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
                lastUsedGradients.put(player.getUniqueId(), new LastGradientInfo(
                        preset.getBlockIds(), direction.name(), "LINEAR"));
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            MessageManager.error(player, "Invalid direction: %s", directionString);
            MessageManager.send(player, Component.text("Valid directions: " +
                    String.join(", ", DIRECTION_SUGGESTIONS), NamedTextColor.GRAY));
        }
    }

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

        if (!saved.getAuthorInfo().equals(player.getUniqueId())) {
            MessageManager.error(player, "You can only delete your own gradients.");
            return;
        }

        storageManager.deleteGradient(player.getUniqueId(), saved.getId());
        MessageManager.success(player, "Gradient '%s' deleted.", name);
    }

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

    public List<String> getSavedGradientSuggestions(Player player) {
        if (storageManager == null) {
            return Collections.emptyList();
        }
        return storageManager.getPlayerGradientNames(player.getUniqueId());
    }

    public void trackLastGradient(UUID playerId, List<String> blockIds, String direction, String mode) {
        lastUsedGradients.put(playerId, new LastGradientInfo(blockIds, direction, mode));
    }

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
