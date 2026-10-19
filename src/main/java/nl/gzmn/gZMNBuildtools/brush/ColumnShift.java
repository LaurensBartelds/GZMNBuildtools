package nl.gzmn.gZMNBuildtools.brush;

/**
 * Plans how a terrain column is rewritten when its surface moves by
 * {@code delta} blocks: the surface layers (and decorations just above) move
 * with it, a gap opened below is filled with the deepest layer block, and a gap
 * left above becomes air.
 */
public final class ColumnShift {

    /** Source marker meaning "place air". */
    public static final int AIR = Integer.MIN_VALUE;

    /** Solid layers below the surface that travel with it (grass, dirt, ...). */
    public static final int LAYER_DEPTH = 4;

    /** Non-solid blocks above the surface that travel with it (plants, snow). */
    public static final int DECORATION_HEIGHT = 2;

    private final int fromY;
    private final int[] sources;

    private ColumnShift(int fromY, int[] sources) {
        this.fromY = fromY;
        this.sources = sources;
    }

    public static ColumnShift plan(int surfaceY, int delta, int minY, int maxY) {
        int bottom = Math.max(minY, surfaceY - LAYER_DEPTH);
        int top = Math.min(maxY, surfaceY + DECORATION_HEIGHT);
        int lo = Math.max(minY, Math.min(bottom, bottom + delta));
        int hi = Math.min(maxY, Math.max(top, top + delta));
        int[] sources = new int[Math.max(0, hi - lo + 1)];
        for (int y = lo; y <= hi; y++) {
            int source;
            if (y >= bottom + delta && y <= top + delta) {
                source = Math.max(minY, Math.min(maxY, y - delta));
            } else if (y < bottom + delta) {
                source = bottom;
            } else {
                source = AIR;
            }
            sources[y - lo] = source;
        }
        return new ColumnShift(lo, sources);
    }

    public int fromY() {
        return fromY;
    }

    public int toY() {
        return fromY + sources.length - 1;
    }

    /** Y to copy the block from for target Y, or {@link #AIR}. */
    public int sourceFor(int y) {
        return sources[y - fromY];
    }

    /** Lowest Y read from the original column. */
    public int minSourceY() {
        int min = Integer.MAX_VALUE;
        for (int s : sources) {
            if (s != AIR) {
                min = Math.min(min, s);
            }
        }
        return min == Integer.MAX_VALUE ? fromY : min;
    }

    /** Highest Y read from the original column. */
    public int maxSourceY() {
        int max = Integer.MIN_VALUE;
        for (int s : sources) {
            max = Math.max(max, s);
        }
        return max == AIR ? fromY : max;
    }
}
