package nl.gzmn.gZMNBuildtools.integration.worldedit.gradient;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Map;

/**
 * Carries a block's state (facing, half, shape, waterlogged, axis, wall
 * connections, ...) over to a different block type, for every property both
 * types share.
 */
public final class StateMapper {

    private StateMapper() {
    }

    @SuppressWarnings("unchecked")
    public static BlockState map(BlockState source, BlockType target) {
        BlockState out = target.getDefaultState();
        for (Map.Entry<Property<?>, Object> entry : source.getStates().entrySet()) {
            Property<Object> property = target.getProperty(entry.getKey().getName());
            if (property != null && property.getValues().contains(entry.getValue())) {
                out = out.with(property, entry.getValue());
            }
        }
        return out;
    }
}
