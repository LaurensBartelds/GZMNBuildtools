package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;

/**
 * Matches solid blocks whose terrain curvature lies within [min, max].
 * Positive curvature is concave (valleys), negative is convex (ridges).
 */
public class CurvatureMask extends AbstractExtentMask {

    private final double min;
    private final double max;
    private final int radius;

    public CurvatureMask(Extent extent, double min, double max, int radius) {
        super(extent);
        this.min = min;
        this.max = max;
        this.radius = radius;
    }

    @Override
    public boolean test(BlockVector3 position) {
        return test(getExtent(), position);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        ExtentTerrainView view = new ExtentTerrainView(extent);
        if (!view.isSolid(position.x(), position.y(), position.z())) {
            return false;
        }
        double curvature = TerrainMath.curvature(view,
                position.x(), position.y(), position.z(), radius);
        return curvature >= min && curvature <= max;
    }

    @Override
    public Mask copy() {
        return new CurvatureMask(getExtent(), min, max, radius);
    }
}
