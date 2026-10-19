package nl.gzmn.gZMNBuildtools.terrain;

/**
 * Heightmap-style terrain analysis (surface height, slope, curvature, depth)
 * shared by the custom masks, patterns and brushes.
 *
 * <p>All lookups are local: heights are searched in a vertical window around
 * the queried Y so overhangs and caves far above or below do not leak in.</p>
 */
public final class TerrainMath {

    /** Returned by {@link #surfaceNear} when no surface exists in the search window. */
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    private static final int[][] RING = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private TerrainMath() {
    }

    /**
     * Finds the Y of the top solid block (solid with a non-solid block above it)
     * in column (x, z) closest to {@code y}, searching at most {@code window}
     * blocks up and down. Returns {@link #NO_SURFACE} if none is found.
     */
    public static int surfaceNear(TerrainView view, int x, int z, int y, int window) {
        int top = Math.min(view.maxY(), y + window);
        int bottom = Math.max(view.minY(), y - window);
        for (int d = 0; d <= window; d++) {
            int up = y + d;
            if (up <= top && isSurface(view, x, up, z)) {
                return up;
            }
            int down = y - d;
            if (d > 0 && down >= bottom && isSurface(view, x, down, z)) {
                return down;
            }
        }
        return NO_SURFACE;
    }

    private static boolean isSurface(TerrainView view, int x, int y, int z) {
        return view.isSolid(x, y, z) && (y >= view.maxY() || !view.isSolid(x, y + 1, z));
    }

    /** Surface height of a column, falling back to {@code fallback} when none is found. */
    public static int heightOr(TerrainView view, int x, int z, int y, int window, int fallback) {
        int h = surfaceNear(view, x, z, y, window);
        return h == NO_SURFACE ? fallback : h;
    }

    /**
     * Terrain slope in degrees (0 = flat, 90 = vertical) at the column of
     * (x, z), using central differences over {@code radius} blocks.
     */
    public static double slopeDegrees(TerrainView view, int x, int y, int z, int radius) {
        int r = Math.max(1, radius);
        int window = searchWindow(r);
        int center = heightOr(view, x, z, y, window, y);
        double dx = (heightOr(view, x + r, z, center, window, center)
                - heightOr(view, x - r, z, center, window, center)) / (2.0 * r);
        double dz = (heightOr(view, x, z + r, center, window, center)
                - heightOr(view, x, z - r, center, window, center)) / (2.0 * r);
        return Math.toDegrees(Math.atan(Math.sqrt(dx * dx + dz * dz)));
    }

    /**
     * Mean height of the 8 neighbours at distance {@code radius} minus the
     * centre height, divided by the radius. Positive values are concave
     * (valleys, hollows); negative values are convex (ridges, peaks).
     */
    public static double curvature(TerrainView view, int x, int y, int z, int radius) {
        int r = Math.max(1, radius);
        int window = searchWindow(r);
        int center = heightOr(view, x, z, y, window, y);
        double sum = 0;
        for (int[] offset : RING) {
            sum += heightOr(view, x + offset[0] * r, z + offset[1] * r, center, window, center);
        }
        return (sum / RING.length - center) / r;
    }

    /**
     * Number of consecutive solid blocks directly above (x, y, z), capped at
     * {@code limit + 1}. A surface block therefore has depth 0.
     */
    public static int depth(TerrainView view, int x, int y, int z, int limit) {
        int depth = 0;
        int maxY = view.maxY();
        for (int cy = y + 1; cy <= maxY && depth <= limit; cy++) {
            if (!view.isSolid(x, cy, z)) {
                break;
            }
            depth++;
        }
        return depth;
    }

    /** Whether no solid block exists anywhere above (x, y, z). */
    public static boolean hasSkyAccess(TerrainView view, int x, int y, int z) {
        int maxY = view.maxY();
        for (int cy = y + 1; cy <= maxY; cy++) {
            if (view.isSolid(x, cy, z)) {
                return false;
            }
        }
        return true;
    }

    private static int searchWindow(int radius) {
        // Steep terrain (up to ~80 degrees) can differ by several blocks per block of distance.
        return Math.max(8, radius * 6);
    }
}
