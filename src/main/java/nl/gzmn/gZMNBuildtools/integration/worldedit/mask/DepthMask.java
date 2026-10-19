package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;

/** Matches solid blocks with between min and max solid blocks directly above them. */
public class DepthMask extends AbstractExtentMask {

    private final int min;
    private final int max;

    public DepthMask(Extent extent, int min, int max) {
        super(extent);
        this.min = min;
        this.max = max;
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
        int depth = TerrainMath.depth(view, position.x(), position.y(), position.z(), max);
        return depth >= min && depth <= max;
    }

    @Override
    public Mask copy() {
        return new DepthMask(getExtent(), min, max);
    }
}
