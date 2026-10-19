package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Matches positions within a spherical radius of at least one block matching
 * the inner mask, e.g. {@code #near[water][4]} for shorelines.
 */
public class NearMask extends AbstractMask {

    private final Mask inner;
    private final int radius;
    private final int[][] offsets;

    public NearMask(Mask inner, int radius) {
        this.inner = inner;
        this.radius = radius;
        this.offsets = sphereOffsets(radius);
    }

    private static int[][] sphereOffsets(int radius) {
        int r2 = radius * radius;
        List<int[]> list = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 <= r2) {
                        list.add(new int[]{x, y, z, d2});
                    }
                }
            }
        }
        // Check closest positions first so typical hits exit early.
        list.sort(Comparator.comparingInt(o -> o[3]));
        return list.toArray(new int[0][]);
    }

    @Override
    public boolean test(BlockVector3 position) {
        int x = position.x();
        int y = position.y();
        int z = position.z();
        for (int[] o : offsets) {
            if (inner.test(BlockVector3.at(x + o[0], y + o[1], z + o[2]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Mask copy() {
        return new NearMask(inner.copy(), radius);
    }
}
