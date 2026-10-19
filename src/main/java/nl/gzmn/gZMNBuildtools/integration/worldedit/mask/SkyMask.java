package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;

/** Matches positions with no solid block anywhere above them. */
public class SkyMask extends AbstractExtentMask {

    public SkyMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(BlockVector3 position) {
        return test(getExtent(), position);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        return TerrainMath.hasSkyAccess(new ExtentTerrainView(extent), position.x(), position.y(), position.z());
    }

    @Override
    public Mask copy() {
        return new SkyMask(getExtent());
    }
}
