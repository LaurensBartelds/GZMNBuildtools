package nl.gzmn.gZMNBuildtools.generate;

import java.util.HashSet;
import java.util.Set;

/**
 * Block-exact shape rasterisers. Every shape reports each block at most once.
 */
public final class Shapes {

    private Shapes() {
    }

    /** Ring torus lying flat (axis vertical) around the centre. */
    public static void torus(int cx, int cy, int cz, double major, double minor, boolean hollow, BlockSink sink) {
        int extent = (int) Math.ceil(major + minor);
        int vertical = (int) Math.ceil(minor);
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                double q = Math.sqrt(x * x + z * z) - major;
                for (int y = -vertical; y <= vertical; y++) {
                    double d = Math.sqrt(q * q + y * y);
                    if (d <= minor && (!hollow || d > minor - 1)) {
                        sink.accept(cx + x, cy + y, cz + z);
                    }
                }
            }
        }
    }

    /** Upright cone with its base on the centre's Y. Hollow cones are open at the bottom. */
    public static void cone(int cx, int cy, int cz, double radius, int height, boolean hollow, BlockSink sink) {
        int r = (int) Math.ceil(radius);
        for (int dy = 0; dy < height; dy++) {
            double layer = radius * (1 - dy / (double) height);
            double inner = radius * (1 - (dy + 1) / (double) height);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > layer + 0.5) {
                        continue;
                    }
                    // A shell block is on the outside of this layer or exposed by the layer above.
                    if (hollow && d <= Math.min(layer - 1, inner) + 0.5 && dy < height - 1) {
                        continue;
                    }
                    sink.accept(cx + x, cy + dy, cz + z);
                }
            }
        }
    }

    /** Upper half of a sphere with its flat side on the centre's Y. */
    public static void dome(int cx, int cy, int cz, double radius, boolean hollow, BlockSink sink) {
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int y = 0; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= radius + 0.5 && (!hollow || d > radius - 0.5)) {
                        sink.accept(cx + x, cy + y, cz + z);
                    }
                }
            }
        }
    }

    /**
     * A parabolic curve from (x1,y1,z1) to (x2,y2,z2) whose midpoint is offset
     * vertically by {@code rise} (positive = arch, negative = sagging cable),
     * drawn with the given thickness.
     */
    public static void curve(int x1, int y1, int z1, int x2, int y2, int z2, double rise, double thickness,
                             BlockSink sink) {
        double length = Math.sqrt(sq(x2 - x1) + sq(y2 - y1) + sq(z2 - z1)) + Math.abs(rise) * 2;
        int steps = Math.max(2, (int) Math.ceil(length * 3));
        Tube tube = new Tube(thickness, sink);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            tube.add(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t + 4 * rise * t * (1 - t), z1 + (z2 - z1) * t);
        }
    }

    /** A helix rising {@code height} blocks while making {@code turns} full turns. */
    public static void spiral(int cx, int cy, int cz, double radius, int height, double turns, double thickness,
                              BlockSink sink) {
        double length = Math.sqrt(sq(2 * Math.PI * radius * turns) + sq(height));
        int steps = Math.max(2, (int) Math.ceil(length * 3));
        Tube tube = new Tube(thickness, sink);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double angle = 2 * Math.PI * turns * t;
            tube.add(cx + radius * Math.cos(angle), cy + height * t, cz + radius * Math.sin(angle));
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Stamps a ball of the given thickness at each point, de-duplicating blocks. */
    private static final class Tube {
        private final double radius;
        private final BlockSink sink;
        private final Set<Long> placed = new HashSet<>();

        Tube(double thickness, BlockSink sink) {
            this.radius = Math.max(0.5, thickness / 2.0);
            this.sink = sink;
        }

        void add(double px, double py, double pz) {
            int bx = (int) Math.round(px);
            int by = (int) Math.round(py);
            int bz = (int) Math.round(pz);
            int r = (int) Math.ceil(radius);
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (x * x + y * y + z * z > radius * radius) {
                            continue;
                        }
                        int ax = bx + x;
                        int ay = by + y;
                        int az = bz + z;
                        if (placed.add(key(ax, ay, az))) {
                            sink.accept(ax, ay, az);
                        }
                    }
                }
            }
        }

        private static long key(int x, int y, int z) {
            return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
        }
    }
}
