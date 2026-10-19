package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/** 3D checkerboard with cells of {@code size} blocks; matches the "even" cells. */
public class CheckerMask extends AbstractMask {

    private final int size;

    public CheckerMask(int size) {
        this.size = size;
    }

    public static boolean even(BlockVector3 position, int size) {
        int cx = Math.floorDiv(position.x(), size);
        int cy = Math.floorDiv(position.y(), size);
        int cz = Math.floorDiv(position.z(), size);
        return ((cx + cy + cz) & 1) == 0;
    }

    @Override
    public boolean test(BlockVector3 position) {
        return even(position, size);
    }

    @Override
    public Mask copy() {
        return this;
    }
}
