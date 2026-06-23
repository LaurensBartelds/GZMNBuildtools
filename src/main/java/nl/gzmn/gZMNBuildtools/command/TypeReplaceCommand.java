package nl.gzmn.gZMNBuildtools.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.typereplace.BlockTypeFamily;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TypeReplaceCommand {

    private static final Logger LOGGER = Logger.getLogger("GZMNBuildtools");

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

            if (BlockTypeFamily.isMaterialGroup(fromMaterial)) {
                List<String> sourceMaterials = BlockTypeFamily.getMaterialGroup(fromMaterial);
                MessageManager.info(player, "Replacing %s → %s (%d materials)…", fromMaterial, toMaterial,
                        sourceMaterials.size());

                int totalReplaced = 0;
                for (String sourceMat : sourceMaterials) {
                    BlockTypeFamily sourceFamily = new BlockTypeFamily(sourceMat);
                    BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

                    if (!sourceFamily.getVariants().isEmpty() && !targetFamily.getVariants().isEmpty()) {
                        ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);
                        if (result.count > 0) {
                            MessageManager.verbose(player, "%s → %s: %d blocks", sourceMat, toMaterial, result.count);
                        }
                        totalReplaced += result.count;
                    }
                }

                MessageManager.success(player, "Replaced %d blocks.", totalReplaced);
                return;
            }

            BlockTypeFamily sourceFamily = new BlockTypeFamily(fromMaterial);
            BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

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

            ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);

            MessageManager.success(player, "Replaced %d blocks.", result.count);

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "An error occurred: %s", e.getMessage());
            LOGGER.log(Level.SEVERE, "Unexpected error during /typereplace " + fromMaterial + " -> " + toMaterial, e);
        }
    }

    public static class ReplaceResult {
        public final int count;

        public ReplaceResult(int count) {
            this.count = count;
        }
    }

    protected com.sk89q.worldedit.regions.Region getSelectionFromPlayer(org.bukkit.entity.Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
        } catch (Exception e) {
            return null;
        }
    }

    private ReplaceResult performTypeReplace(com.sk89q.worldedit.entity.Player actor, Region region,
                                             BlockTypeFamily sourceFamily, BlockTypeFamily targetFamily) {
        int count = 0;

        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        try (EditSession editSession = localSession.createEditSession(actor)) {
            Set<BlockType> sourceBlocks = new HashSet<>(sourceFamily.getVariants().values());

            for (BlockVector3 position : region) {
                BlockState currentBlock = editSession.getBlock(position);
                BlockType currentType = currentBlock.getBlockType();

                if (sourceBlocks.contains(currentType)) {
                    String sourceVariant = BlockTypeFamily.getVariantType(currentType);
                    BlockType targetType = BlockTypeFamily.mapVariant(currentType, targetFamily);

                    if (targetType != null && !targetType.id().equals("minecraft:air")) {
                        BlockState newState = targetType.getDefaultState();

                        String targetVariant = BlockTypeFamily.getVariantType(targetType);

                        boolean isCrossTypeConnectorConversion = isVerticalConnector(sourceVariant) &&
                                isVerticalConnector(targetVariant) &&
                                !sourceVariant.equals(targetVariant);

                        try {
                            newState = preserveBlockProperties(currentBlock, newState, isCrossTypeConnectorConversion);
                        } catch (Exception e) {
                            newState = targetType.getDefaultState();
                            LOGGER.log(Level.WARNING, "Failed to preserve block properties mapping "
                                    + currentType.id() + " -> " + targetType.id()
                                    + "; using default state", e);
                        }

                        if (!newState.getBlockType().id().equals("minecraft:air")) {
                            boolean placed = editSession.setBlock(position, newState);
                            count++;
                        }
                    }
                }
            }

            localSession.remember(editSession);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while applying type replacement", e);
        }

        return new ReplaceResult(count);
    }

    private boolean isVerticalConnector(String variantType) {
        return variantType.equals("wall") || variantType.equals("fence") || variantType.equals("bars");
    }

    private BlockState preserveBlockProperties(BlockState source, BlockState target,
                                               boolean skipDirectionalConnections) {
        BlockState result = target;

        Set<String> directionalProperties = Set.of("north", "south", "east", "west", "up");

        var sourceProperties = source.getStates();

        for (var entry : sourceProperties.entrySet()) {
            try {
                String propertyName = entry.getKey().getName();

                if (skipDirectionalConnections && directionalProperties.contains(propertyName)) {
                    continue;
                }

                if (target.getBlockType().hasProperty(entry.getKey())) {
                    @SuppressWarnings("unchecked")
                    var property = (com.sk89q.worldedit.registry.state.Property<Object>) entry.getKey();
                    result = result.with(property, entry.getValue());
                }
            } catch (Exception e) {
                // A single incompatible property should not abort the whole block;
                // skip it but record why at a fine level for diagnostics.
                LOGGER.log(Level.FINE, "Skipped incompatible property '" + entry.getKey().getName()
                        + "' when mapping to " + target.getBlockType().id(), e);
            }
        }

        return result;
    }
}
