package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;

/** Matches solid blocks whose terrain slope (in degrees) lies within [min, max]. */
public class SlopeMask extends AbstractExtentMask {

    private final double minDegrees;
    private final double maxDegrees;
    private final int radius;

    public SlopeMask(Extent extent, double minDegrees, double maxDegrees, int radius) {
        super(extent);
        this.minDegrees = minDegrees;
        this.maxDegrees = maxDegrees;
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
        double slope = TerrainMath.slopeDegrees(view,
                position.x(), position.y(), position.z(), radius);
        return slope >= minDegrees && slope <= maxDegrees;
    }

    @Override
    public Mask copy() {
        return new SlopeMask(getExtent(), minDegrees, maxDegrees, radius);
    }
}
