package nl.gzmn.gZMNBuildtools.modules.terrainbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.SnowSmooth;
import nl.gzmn.gZMNBuildtools.shared.terrain.ExtentTerrainView;

/**
 * Smooths snow cover inside the brush cylinder: every column's snow layers move
 * towards the neighbourhood average; with {@code fill} bare ground gains snow
 * and snow averaging out to nothing is cleared.
 */
public class SnowBrush implements Brush {

    private static final BlockType SNOW_LAYER = BlockTypes.get("minecraft:snow_layer");

    private final double strength;
    private final boolean fill;

    public SnowBrush(double strength, boolean fill) {
        this.strength = strength;
        this.fill = fill;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        int radius = Math.max(1, (int) Math.round(size));
        ExtentTerrainView view = new ExtentTerrainView(editSession);
        int top = Math.min(view.maxY(), position.y() + radius);
        int bottom = Math.max(view.minY(), position.y() - radius);
        SnowSmooth.ColumnTop columnTop = (x, z) -> {
            for (int y = top; y >= bottom; y--) {
                if (view.isSolid(x, y, z)) {
                    return y;
                }
            }
            return bottom - 1;
        };
        SnowSmooth.ColumnSnow snow = (x, z) -> {
            int y = columnTop.topY(x, z);
            if (y < view.minY() || y >= view.maxY()) {
                return 0;
            }
            return layersOf(editSession.getBlock(x, y + 1, z));
        };
        SnowSmooth.Plan plan = SnowSmooth.plan(position.x(), position.z(), radius, columnTop, snow,
                strength, fill, view.minY(), view.maxY());
        for (var entry : plan.newLayers().entrySet()) {
            SnowSmooth.Cell cell = entry.getKey();
            editSession.setBlock(cell.x(), cell.y(), cell.z(), snowState(entry.getValue()));
        }
        for (SnowSmooth.Cell cell : plan.remove()) {
            editSession.setBlock(cell.x(), cell.y(), cell.z(), BlockTypes.AIR.getDefaultState());
        }
    }

    private static int layersOf(BlockState state) {
        if (state.getBlockType() != SNOW_LAYER) {
            return 0;
        }
        Property<Object> property = property(state);
        Object value = property == null ? null : state.getState(property);
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static BlockState snowState(int layers) {
        BlockState state = SNOW_LAYER.getDefaultState();
        Property<Object> property = property(state);
        if (property == null) {
            return state;
        }
        for (Object v : property.getValues()) {
            if (v instanceof Number n && n.intValue() == layers) {
                return state.with(property, v);
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static Property<Object> property(BlockState state) {
        return (Property<Object>) state.getBlockType().getProperty("layers");
    }
}
