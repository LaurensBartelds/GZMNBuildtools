package nl.gzmn.gZMNBuildtools.integration.worldedit.gradient;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import nl.gzmn.gZMNBuildtools.gradient.smart.BlockSource;

/** {@link BlockSource} over a WorldEdit/FAWE extent. */
public final class ExtentBlockSource implements BlockSource {

    private final Extent extent;
    private final int minY;
    private final int maxY;

    public ExtentBlockSource(Extent extent) {
        this.extent = extent;
        this.minY = extent.getMinY();
        this.maxY = extent.getMaxY();
    }

    public static String idOf(BlockState state) {
        if (state.getBlockType().getMaterial().isAir()) {
            return null;
        }
        String id = state.getBlockType().id();
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    @Override
    public String blockAt(int x, int y, int z) {
        if (y < minY || y > maxY) {
            return null;
        }
        return idOf(extent.getBlock(x, y, z));
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
