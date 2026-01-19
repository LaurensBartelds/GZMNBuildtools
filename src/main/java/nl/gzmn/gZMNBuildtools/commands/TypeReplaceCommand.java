package nl.gzmn.gZMNBuildtools.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.util.BlockTypeFamily;
import nl.gzmn.gZMNBuildtools.util.MessageManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command to replace entire block type families (e.g., all cobblestone variants
 * to copper variants)
 */
public class TypeReplaceCommand {

    public List<String> getSuggestions() {
        List<String> suggestions = new ArrayList<>(BlockTypeFamily.getAllMaterialNames());
        Collections.sort(suggestions);
        return suggestions;
    }

    public void execute(Player player, String fromMaterialArg, String toMaterialArg) {
        String fromMaterial = fromMaterialArg.toLowerCase();
        String toMaterial = toMaterialArg.toLowerCase();

        try {
            Region region = getSelectionFromPlayer(player);
            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);

            // Check if source is a material group (e.g., "all_copper")
            if (BlockTypeFamily.isMaterialGroup(fromMaterial)) {
                List<String> sourceMaterials = BlockTypeFamily.getMaterialGroup(fromMaterial);
                MessageManager.info(player, "Replacing %s → %s (%d materials)…", fromMaterial, toMaterial,
                        sourceMaterials.size());

                int totalReplaced = 0;
                boolean anyVerticalConnectors = false;
                for (String sourceMat : sourceMaterials) {
                    BlockTypeFamily sourceFamily = new BlockTypeFamily(sourceMat);
                    BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

                    if (!sourceFamily.getVariants().isEmpty() && !targetFamily.getVariants().isEmpty()) {
                        ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);
                        if (result.count > 0) {
                            MessageManager.verbose(player, "%s → %s: %d blocks", sourceMat, toMaterial, result.count);
                        }
                        totalReplaced += result.count;
                        if (result.hasVerticalConnectors) {
                            anyVerticalConnectors = true;
                        }
                    }
                }

                MessageManager.success(player, "Replaced %d blocks.", totalReplaced);
                if (anyVerticalConnectors) {
                    MessageManager.warn(player, "Tip: run //fixconnect to fix wall/fence/bar connections.");
                }
                return;
            }

            // Single material replacement
            BlockTypeFamily sourceFamily = new BlockTypeFamily(fromMaterial);
            BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

            // Check if families have any variants
            if (sourceFamily.getVariants().isEmpty()) {
                MessageManager.error(player, "Unknown material: %s", fromMaterial);
                return;
            }
            if (targetFamily.getVariants().isEmpty()) {
                MessageManager.error(player, "Unknown material: %s", toMaterial);
                return;
            }

            MessageManager.info(player, "Replacing %s → %s…", fromMaterial, toMaterial);
            MessageManager.verbose(player, "Source variants: %s", sourceFamily.getVariants().keySet());
            MessageManager.verbose(player, "Target variants: %s", targetFamily.getVariants().keySet());

            // Perform the replacement
            ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);

            MessageManager.success(player, "Replaced %d blocks.", result.count);
            if (result.hasVerticalConnectors) {
                MessageManager.warn(player, "Tip: run //fixconnect to fix wall/fence/bar connections.");
            }

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Result of a type replacement operation
     */
    public static class ReplaceResult {
        public final int count;
        public final boolean hasVerticalConnectors;

        public ReplaceResult(int count, boolean hasVerticalConnectors) {
            this.count = count;
            this.hasVerticalConnectors = hasVerticalConnectors;
        }
    }

    /**
     * Separated for testability — override in tests to avoid mocking WorldEdit
     * internals.
     */
    protected com.sk89q.worldedit.regions.Region getSelectionFromPlayer(org.bukkit.entity.Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Perform the type replacement operation
     */
    private ReplaceResult performTypeReplace(com.sk89q.worldedit.entity.Player actor, Region region,
            BlockTypeFamily sourceFamily, BlockTypeFamily targetFamily) {
        int count = 0;
        boolean hasVerticalConnectors = false;

        // Get the player's LocalSession for undo support
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        try (EditSession editSession = localSession.createEditSession(actor)) {
            // Get all source block types in this family
            Set<BlockType> sourceBlocks = new HashSet<>(sourceFamily.getVariants().values());

            // Iterate through all blocks in the region
            for (BlockVector3 position : region) {
                BlockState currentBlock = editSession.getBlock(position);
                BlockType currentType = currentBlock.getBlockType();

                // Check if this block belongs to the source family
                if (sourceBlocks.contains(currentType)) {
                    // Map to the equivalent target variant
                    String variantType = BlockTypeFamily.getVariantType(currentType);
                    BlockType targetType = BlockTypeFamily.mapVariant(currentType, targetFamily);

                    if (targetType != null && !targetType.id().equals("minecraft:air")) {
                        // Preserve block states where possible (rotation, waterlogging, etc.)
                        BlockState newState = targetType.getDefaultState();

                        // Skip property preservation for vertical connector conversions
                        // (bars/wall/fence)
                        // These have incompatible property types that can create invalid block states
                        boolean isVerticalConnectorConversion = (variantType.equals("bars")
                                || variantType.equals("wall") || variantType.equals("fence")) &&
                                (targetType.id().contains("_wall") || targetType.id().contains("_bars")
                                        || targetType.id().contains("_fence"));

                        if (!isVerticalConnectorConversion) {
                            // Try to preserve properties that exist in both blocks
                            try {
                                newState = preserveBlockProperties(currentBlock, newState);
                            } catch (Exception e) {
                                newState = targetType.getDefaultState();
                            }
                        } else {
                            hasVerticalConnectors = true;
                        }

                        // Safety check: don't set blocks to air
                        if (!newState.getBlockType().id().equals("minecraft:air")) {
                            editSession.setBlock(position, newState);
                            count++;
                        }
                    }
                }
            }

            // Record the edit session to history for undo support
            localSession.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ReplaceResult(count, hasVerticalConnectors);
    }

    /**
     * Attempt to preserve block properties between source and target blocks
     */
    private BlockState preserveBlockProperties(BlockState source, BlockState target) {
        BlockState result = target;

        // Get all properties from source
        var sourceProperties = source.getStates();

        // Try to apply each property to the target if it exists
        for (var entry : sourceProperties.entrySet()) {
            try {
                if (target.getBlockType().hasProperty(entry.getKey())) {
                    @SuppressWarnings("unchecked")
                    var property = (com.sk89q.worldedit.registry.state.Property<Object>) entry.getKey();
                    result = result.with(property, entry.getValue());
                }
            } catch (Exception e) {
                // Property doesn't exist or value is incompatible, skip it
            }
        }

        return result;
    }
}
