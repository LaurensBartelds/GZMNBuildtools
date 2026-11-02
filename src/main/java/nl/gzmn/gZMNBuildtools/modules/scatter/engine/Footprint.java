package nl.gzmn.gZMNBuildtools.modules.scatter.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a prefab touches the ground and how much room it takes, relative to
 * its anchor (the middle of its bottom layer).
 *
 * @param base   the (x, z) columns of the bottom layer
 * @param reach  how far it spreads sideways from the anchor
 * @param height how tall it is
 */
public record Footprint(List<int[]> base, double reach, int height) {

    /** The base columns turned {@code turns} quarter turns (x, z) -> (-z, x) and optionally mirrored in x first. */
    public List<int[]> base(int turns, boolean flip) {
        List<int[]> out = new ArrayList<>(base.size());
        for (int[] c : base) {
            out.add(turn(flip ? -c[0] : c[0], c[1], turns));
        }
        return out;
    }

    /** One rotation step is (x, z) -> (-z, x), the same as WorldEdit's {@code rotateY(-90)}. */
    public static int[] turn(int x, int z, int turns) {
        int tx = x;
        int tz = z;
        for (int i = 0; i < Math.floorMod(turns, 4); i++) {
            int nx = -tz;
            tz = tx;
            tx = nx;
        }
        return new int[]{tx, tz};
    }
}
