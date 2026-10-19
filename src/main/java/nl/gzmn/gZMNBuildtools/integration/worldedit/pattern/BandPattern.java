package nl.gzmn.gZMNBuildtools.integration.worldedit.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractExtentPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;
import nl.gzmn.gZMNBuildtools.terrain.TerrainView;

import java.util.List;

/**
 * Chooses one of several patterns by a per-position terrain metric:
 * slope, depth below the surface, or height (with optional noise jitter).
 */
public class BandPattern extends AbstractExtentPattern {

    public enum Metric {
        SLOPE, DEPTH, HEIGHT
    }

    private final Metric metric;
    private final List<Band> bands;
    private final int radius;
    private final double jitter;
    private final long seed;

    public BandPattern(Extent extent, Metric metric, List<Band> bands, int radius, double jitter, long seed) {
        super(extent);
        if (bands.isEmpty()) {
            throw new IllegalArgumentException("At least one band is required");
        }
        this.metric = metric;
        this.bands = Band.sorted(bands);
        this.radius = radius;
        this.jitter = jitter;
        this.seed = seed;
    }

    double value(TerrainView view, BlockVector3 position) {
        return switch (metric) {
            case SLOPE -> TerrainMath.slopeDegrees(view, position.x(), position.y(), position.z(), radius);
            case DEPTH -> TerrainMath.depth(view, position.x(), position.y(), position.z(),
                    (int) Math.ceil(bands.get(bands.size() - 1).threshold()));
            case HEIGHT -> position.y() + jitter * Jitter.value(seed, position.x(), position.z());
        };
    }

    private Pattern choose(Extent extent, BlockVector3 position) {
        return Band.select(bands, value(new ExtentTerrainView(extent), position));
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return choose(getExtent(), position).applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return choose(extent, get).apply(extent, get, set);
    }
}
