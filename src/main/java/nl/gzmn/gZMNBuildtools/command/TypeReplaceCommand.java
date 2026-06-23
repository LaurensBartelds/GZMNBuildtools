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
import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import nl.gzmn.gZMNBuildtools.typereplace.BlockFamily;
import nl.gzmn.gZMNBuildtools.typereplace.registry.InMemoryBlockFamilyRegistry;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TypeReplaceCommand {

    private static final Logger LOGGER = Logger.getLogger("GZMNBuildtools");

    private Messages messages = AdventureMessages.basic();
    private BlockFamilyRegistry blockFamilies = InMemoryBlockFamilyRegistry.bundled();

    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    public void setBlockFamilies(BlockFamilyRegistry blockFamilies) {
        this.blockFamilies = blockFamilies;
    }

    public List<String> getSuggestions() {
        List<String> suggestions = new ArrayList<>(blockFamilies.materialNames());
        Collections.sort(suggestions);
        return suggestions;
    }

    public void execute(Player player, String fromMaterialArg, String toMaterialArg) {
        String fromMaterial = fromMaterialArg.toLowerCase();
        String toMaterial = toMaterialArg.toLowerCase();

        try {
            Region region = getSelectionFromPlayer(player);
            if (region == null) {
                messages.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);

            if (blockFamilies.isGroup(fromMaterial)) {
                List<String> sourceMaterials = blockFamilies.group(fromMaterial);
                messages.info(player, "Replacing %s → %s (%d materials)…", fromMaterial, toMaterial,
                        sourceMaterials.size());

                int totalReplaced = 0;
                for (String sourceMat : sourceMaterials) {
                    BlockFamily sourceFamily = blockFamilies.family(sourceMat);
                    BlockFamily targetFamily = blockFamilies.family(toMaterial);

                    if (!sourceFamily.getVariants().isEmpty() && !targetFamily.getVariants().isEmpty()) {
                        ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);
                        if (result.count > 0) {
                            messages.verbose(player, "%s → %s: %d blocks", sourceMat, toMaterial, result.count);
                        }
                        totalReplaced += result.count;
                    }
                }

                messages.success(player, "Replaced %d blocks.", totalReplaced);
                return;
            }

            BlockFamily sourceFamily = blockFamilies.family(fromMaterial);
            BlockFamily targetFamily = blockFamilies.family(toMaterial);

            if (sourceFamily.getVariants().isEmpty()) {
                messages.error(player, "Unknown material: %s", fromMaterial);
                return;
            }
            if (targetFamily.getVariants().isEmpty()) {
                messages.error(player, "Unknown material: %s", toMaterial);
                return;
            }

            messages.info(player, "Replacing %s → %s…", fromMaterial, toMaterial);
            messages.verbose(player, "Source variants: %s", sourceFamily.getVariants().keySet());
            messages.verbose(player, "Target variants: %s", targetFamily.getVariants().keySet());

            ReplaceResult result = performTypeReplace(actor, region, sourceFamily, targetFamily);

            messages.success(player, "Replaced %d blocks.", result.count);

        } catch (IncompleteRegionException e) {
            messages.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            messages.error(player, "An error occurred: %s", e.getMessage());
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
                                             BlockFamily sourceFamily, BlockFamily targetFamily) {
        int count = 0;

        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        try (EditSession editSession = localSession.createEditSession(actor)) {
            Set<BlockType> sourceBlocks = new HashSet<>(sourceFamily.getVariants().values());

            for (BlockVector3 position : region) {
                BlockState currentBlock = editSession.getBlock(position);
                BlockType currentType = currentBlock.getBlockType();

                if (sourceBlocks.contains(currentType)) {
                    String sourceVariant = blockFamilies.variantType(currentType);
                    BlockType targetType = blockFamilies.mapVariant(currentType, targetFamily);

                    if (targetType != null && !targetType.id().equals("minecraft:air")) {
                        BlockState newState = targetType.getDefaultState();

                        String targetVariant = blockFamilies.variantType(targetType);

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
