package nl.gzmn.gZMNBuildtools.integration.worldedit.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Cycles through patterns by a cell index derived from the position:
 * checkerboards and stripes along an axis or diagonal.
 */
public class CyclePattern extends AbstractPattern {

    private final List<Pattern> patterns;
    private final ToIntFunction<BlockVector3> cell;

    public CyclePattern(List<Pattern> patterns, ToIntFunction<BlockVector3> cell) {
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("At least one pattern is required");
        }
        this.patterns = List.copyOf(patterns);
        this.cell = cell;
    }

    public static CyclePattern checker(int size, List<Pattern> patterns) {
        return new CyclePattern(patterns, p -> Math.floorDiv(p.x(), size)
                + Math.floorDiv(p.y(), size) + Math.floorDiv(p.z(), size));
    }

    public static CyclePattern stripes(String axis, int width, List<Pattern> patterns) {
        ToIntFunction<BlockVector3> coordinate = switch (axis) {
            case "x" -> BlockVector3::x;
            case "y" -> BlockVector3::y;
            case "z" -> BlockVector3::z;
            case "xz" -> p -> p.x() + p.z();
            case "-xz" -> p -> p.x() - p.z();
            default -> throw new IllegalArgumentException("Unknown axis " + axis);
        };
        return new CyclePattern(patterns, p -> Math.floorDiv(coordinate.applyAsInt(p), width));
    }

    Pattern patternAt(BlockVector3 position) {
        return patterns.get(Math.floorMod(cell.applyAsInt(position), patterns.size()));
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return patternAt(position).applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return patternAt(get).apply(extent, get, set);
    }
}
