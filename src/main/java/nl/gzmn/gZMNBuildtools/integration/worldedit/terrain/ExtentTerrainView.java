package nl.gzmn.gZMNBuildtools.integration.worldedit.terrain;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import nl.gzmn.gZMNBuildtools.terrain.TerrainView;

/** {@link TerrainView} backed by a WorldEdit {@link Extent}. */
public final class ExtentTerrainView implements TerrainView {

    private final Extent extent;
    private final int minY;
    private final int maxY;

    public ExtentTerrainView(Extent extent) {
        this.extent = extent;
        this.minY = extent.getMinY();
        this.maxY = extent.getMaxY();
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        if (y < minY || y > maxY) {
            return false;
        }
        return SolidBlockMask.isSolid(extent.getBlock(x, y, z));
    }

    @Override
    public int minY() {
        return minY;
    }

    @Override
    public int maxY() {
        return maxY;
    }
}
