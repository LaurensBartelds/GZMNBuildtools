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
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.InterpolationMode;
import nl.gzmn.gZMNBuildtools.ui.GradientUIManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command to apply gradients to WorldEdit selections
 */
public class GradientCommand {

    private final GradientUIManager uiManager;

    private static final List<String> BLOCK_SUGGESTIONS = Arrays.asList(
        "stone,cobblestone,andesite",
        "white_wool,light_gray_wool,gray_wool,black_wool",
        "oak_planks,spruce_planks,dark_oak_planks",
        "sandstone,red_sandstone",
        "grass_block,dirt,coarse_dirt"
    );

    private static final List<String> DIRECTION_SUGGESTIONS = Arrays.asList(
        "VERTICAL_UP", "VERTICAL_DOWN", "HORIZONTAL_X", "HORIZONTAL_Z", "RADIAL"
    );

    private static final List<String> MODE_SUGGESTIONS = Arrays.asList(
        "LINEAR", "SMOOTH", "DISCRETE"
    );

    public GradientCommand(GradientUIManager uiManager) {
        this.uiManager = uiManager;
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

    /**
     * Open the gradient UI for a player
     */
    public void openUI(Player player) {
        if (uiManager != null) {
            uiManager.openGradientUI(player);
        } else {
            player.sendMessage(Component.text("Gradient UI is not available.", NamedTextColor.RED));
        }
    }

    /**
     * Execute the gradient command with arguments
     */
    public void execute(Player player, String blocksString, String directionString, String modeString) {
        try {
            // Parse direction
            GradientDirection direction = GradientDirection.valueOf(directionString.toUpperCase());
            InterpolationMode mode = InterpolationMode.valueOf(modeString.toUpperCase());

            // Parse gradient
            GradientDefinition gradient = GradientDefinition.parse(blocksString, direction, mode);

            // Get WorldEdit selection
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());

            if (region == null) {
                player.sendMessage(Component.text("Please make a WorldEdit selection first.", NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("Applying gradient...", NamedTextColor.YELLOW));

            // Apply gradient
            int affected = applyGradient(actor, region, gradient);

            player.sendMessage(Component.text("Gradient applied to " + affected + " blocks!", NamedTextColor.GREEN));

        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            player.sendMessage(Component.text("Valid directions: VERTICAL_UP, VERTICAL_DOWN, HORIZONTAL_X, HORIZONTAL_Z, RADIAL", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Valid modes: LINEAR, SMOOTH, DISCRETE", NamedTextColor.GRAY));
        } catch (IncompleteRegionException e) {
            player.sendMessage(Component.text("Please make a WorldEdit selection first.", NamedTextColor.RED));
        } catch (Exception e) {
            player.sendMessage(Component.text("An error occurred: " + e.getMessage(), NamedTextColor.RED));
            e.printStackTrace();
        }
    }

    /**
     * Apply a gradient to a region
     */
    public int applyGradient(com.sk89q.worldedit.entity.Player actor, Region region, GradientDefinition gradient) {
        int count = 0;

        // Get the player's LocalSession for undo support
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        try (EditSession editSession = localSession.createEditSession(actor)) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            // Calculate gradient bounds based on direction
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
                    // For radial, we'll use distance from center
                    BlockVector3 center = region.getCenter().toBlockPoint();
                    minValue = 0;
                    maxValue = Math.max(
                        Math.max(Math.abs(max.x() - center.x()), Math.abs(min.x() - center.x())),
                        Math.max(Math.abs(max.z() - center.z()), Math.abs(min.z() - center.z()))
                    );
                    break;
                default:
                    minValue = 0;
                    maxValue = 1;
            }

            double range = maxValue - minValue;
            if (range == 0) range = 1; // Avoid division by zero

            // Apply gradient to each block
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

                // Normalize to 0-1 range
                double normalizedPosition = (value - minValue) / range;

                // Get block at this position
                BlockType blockType = gradient.getBlockAt(normalizedPosition);

                if (blockType != null) {
                    editSession.setBlock(position, blockType.getDefaultState());
                    count++;
                }
            }

            // Record the edit session to history for undo support
            localSession.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}
