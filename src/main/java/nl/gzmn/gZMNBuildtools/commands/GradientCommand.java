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
import nl.gzmn.gZMNBuildtools.util.MessageManager;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.InterpolationMode;
import nl.gzmn.gZMNBuildtools.ui.GradientUIManager;
import org.bukkit.entity.Player;

import java.util.*;

public class GradientCommand {

    private final GradientUIManager uiManager;

    private static final List<String> BLOCK_SUGGESTIONS = Arrays.asList(
            "stone,cobblestone,andesite",
            "white_wool,light_gray_wool,gray_wool,black_wool",
            "oak_planks,spruce_planks,dark_oak_planks",
            "sandstone,red_sandstone",
            "grass_block,dirt,coarse_dirt");

    private static final List<String> DIRECTION_SUGGESTIONS = Arrays.asList(
            "VERTICAL_UP", "VERTICAL_DOWN", "HORIZONTAL_X", "HORIZONTAL_Z", "RADIAL");

    private static final List<String> MODE_SUGGESTIONS = Arrays.asList(
            "LINEAR", "SMOOTH", "DISCRETE");

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

    public void openUI(Player player) {
        if (uiManager != null) {
            uiManager.openGradientUI(player);
        } else {
            MessageManager.error(player, "Gradient UI is not available.");
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
            MessageManager.send(player, Component.text("Valid modes: LINEAR, SMOOTH, DISCRETE", NamedTextColor.GRAY));
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

                BlockType blockType = gradient.getBlockAt(normalizedPosition);

                if (blockType != null) {
                    editSession.setBlock(position, blockType.getDefaultState());
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
}
