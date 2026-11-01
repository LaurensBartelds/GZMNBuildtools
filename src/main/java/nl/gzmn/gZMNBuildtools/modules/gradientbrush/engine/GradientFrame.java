package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;

/**
 * Maps a block position to its place (0..1) along the gradient, measured
 * across a build's bounds so every stroke on the same build agrees.
 */
public final class GradientFrame {

    private final Bounds bounds;
    private final GradientAxis axis;
    private final double from;
    private final double to;
    private final double centerX;
    private final double centerZ;

    private GradientFrame(Bounds bounds, GradientAxis axis, Double rangeMin, Double rangeMax) {
        this.bounds = bounds;
        this.axis = axis;
        this.centerX = (bounds.minX() + bounds.maxX()) / 2.0;
        this.centerZ = (bounds.minZ() + bounds.maxZ()) / 2.0;
        double lo;
        double hi;
        switch (axis) {
            case UP, DOWN -> {
                lo = bounds.minY();
                hi = bounds.maxY();
            }
            case X, NEG_X -> {
                lo = bounds.minX();
                hi = bounds.maxX();
            }
            case Z, NEG_Z -> {
                lo = bounds.minZ();
                hi = bounds.maxZ();
            }
            default -> {
                lo = 0;
                double dx = Math.max(centerX - bounds.minX(), bounds.maxX() - centerX);
                double dz = Math.max(centerZ - bounds.minZ(), bounds.maxZ() - centerZ);
                hi = Math.sqrt(dx * dx + dz * dz);
            }
        }
        this.from = rangeMin != null ? rangeMin : lo;
        this.to = rangeMax != null ? rangeMax : hi;
    }

    /**
     * @param range optional fixed [min, max] along the axis (world coordinates
     *              or radial distance) instead of the build's own extent
     */
    public static GradientFrame of(Bounds bounds, GradientAxis axis, double[] range) {
        GradientAxis resolved = axis == GradientAxis.AUTO ? autoAxis(bounds) : axis;
        return range == null
                ? new GradientFrame(bounds, resolved, null, null)
                : new GradientFrame(bounds, resolved, range[0], range[1]);
    }

    /**
     * Walls, towers and anything at least 4 high run bottom to top; flat
     * builds (floors, roads, roofs) run along their longer side.
     */
    public static GradientAxis autoAxis(Bounds bounds) {
        int width = Math.max(bounds.sizeX(), bounds.sizeZ());
        if (bounds.sizeY() >= 4 || bounds.sizeY() >= 0.25 * width) {
            return GradientAxis.UP;
        }
        return bounds.sizeX() >= bounds.sizeZ() ? GradientAxis.X : GradientAxis.Z;
    }

    public GradientAxis axis() {
        return axis;
    }

    public Bounds bounds() {
        return bounds;
    }

    public double t(int x, int y, int z) {
        double v = switch (axis) {
            case UP, DOWN -> y;
            case X, NEG_X -> x;
            case Z, NEG_Z -> z;
            default -> Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
        };
        double t = to == from ? 0.5 : (v - from) / (to - from);
        t = Math.max(0, Math.min(1, t));
        return switch (axis) {
            case DOWN, NEG_X, NEG_Z, INWARD -> 1 - t;
            default -> t;
        };
    }
}
