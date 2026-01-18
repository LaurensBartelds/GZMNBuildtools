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
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command to replace entire block type families (e.g., all cobblestone variants to copper variants)
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

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);

        try {
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
            if (region == null) {
                player.sendMessage(Component.text("Please make a WorldEdit selection first.", NamedTextColor.RED));
                return;
            }

            // Check if source is a material group (e.g., "all_copper")
            if (BlockTypeFamily.isMaterialGroup(fromMaterial)) {
                List<String> sourceMaterials = BlockTypeFamily.getMaterialGroup(fromMaterial);
                player.sendMessage(Component.text("Replacing " + fromMaterial + " group (" + sourceMaterials.size() + " materials) with " + toMaterial + "...", NamedTextColor.YELLOW));

                int totalReplaced = 0;
                for (String sourceMat : sourceMaterials) {
                    BlockTypeFamily sourceFamily = new BlockTypeFamily(sourceMat);
                    BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

                    if (!sourceFamily.getVariants().isEmpty() && !targetFamily.getVariants().isEmpty()) {
                        int replaced = performTypeReplace(actor, region, sourceFamily, targetFamily);
                        if (replaced > 0) {
                            player.sendMessage(Component.text("  " + sourceMat + " → " + toMaterial + ": " + replaced + " blocks", NamedTextColor.GRAY));
                        }
                        totalReplaced += replaced;
                    }
                }

                player.sendMessage(Component.text("Successfully replaced " + totalReplaced + " blocks total!", NamedTextColor.GREEN));
                return;
            }

            // Single material replacement
            BlockTypeFamily sourceFamily = new BlockTypeFamily(fromMaterial);
            BlockTypeFamily targetFamily = new BlockTypeFamily(toMaterial);

            // Check if families have any variants
            if (sourceFamily.getVariants().isEmpty()) {
                player.sendMessage(Component.text("Unknown material: " + fromMaterial, NamedTextColor.RED));
                return;
            }
            if (targetFamily.getVariants().isEmpty()) {
                player.sendMessage(Component.text("Unknown material: " + toMaterial, NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("Replacing " + fromMaterial + " family with " + toMaterial + " family...", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Source variants: " + sourceFamily.getVariants().keySet(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("Target variants: " + targetFamily.getVariants().keySet(), NamedTextColor.GRAY));

            // Perform the replacement
            int replaced = performTypeReplace(actor, region, sourceFamily, targetFamily);

            player.sendMessage(Component.text("Successfully replaced " + replaced + " blocks!", NamedTextColor.GREEN));

        } catch (IncompleteRegionException e) {
            player.sendMessage(Component.text("Please make a WorldEdit selection first.", NamedTextColor.RED));
        } catch (Exception e) {
            player.sendMessage(Component.text("An error occurred: " + e.getMessage(), NamedTextColor.RED));
            e.printStackTrace();
        }
    }

    /**
     * Perform the type replacement operation
     */
    private int performTypeReplace(com.sk89q.worldedit.entity.Player actor, Region region,
                                   BlockTypeFamily sourceFamily, BlockTypeFamily targetFamily) {
        int count = 0;

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
                    BlockType targetType = BlockTypeFamily.mapVariant(currentType, targetFamily);

                    if (targetType != null) {
                        // Preserve block states where possible (rotation, waterlogging, etc.)
                        BlockState newState = targetType.getDefaultState();

                        // Try to preserve properties that exist in both blocks
                        try {
                            newState = preserveBlockProperties(currentBlock, newState);
                        } catch (Exception e) {
                            // If property preservation fails, just use default state
                        }

                        editSession.setBlock(position, newState);
                        count++;
                    }
                }
            }

            // Record the edit session to history for undo support
            localSession.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
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
