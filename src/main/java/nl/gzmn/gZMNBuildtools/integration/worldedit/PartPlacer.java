package nl.gzmn.gZMNBuildtools.integration.worldedit;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.List;

/**
 * Turns generated parts into block states: picks the part's pattern, rotates
 * log-like blocks to the part's axis and makes leaves persistent so they
 * never decay away from their logs.
 */
public final class PartPlacer {

    private final List<Pattern> patterns;

    public PartPlacer(List<Pattern> patterns) {
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("At least one pattern is required");
        }
        this.patterns = List.copyOf(patterns);
    }

    @SuppressWarnings("unchecked")
    public BlockState state(int x, int y, int z, int pattern, char axis) {
        BaseBlock block = patterns.get(Math.min(pattern, patterns.size() - 1)).applyBlock(BlockVector3.at(x, y, z));
        if (block == null) {
            return null;
        }
        BlockState state = block.toImmutableState();
        if (axis != 0) {
            Property<Object> axisProperty = state.getBlockType().getProperty("axis");
            if (axisProperty != null) {
                for (Object value : axisProperty.getValues()) {
                    if (value.toString().equals(String.valueOf(axis))) {
                        state = state.with(axisProperty, value);
                    }
                }
            }
        }
        Property<Object> persistent = state.getBlockType().getProperty("persistent");
        if (persistent != null) {
            state = state.with(persistent, Boolean.TRUE);
        }
        return state;
    }
}
