package nl.gzmn.gZMNBuildtools.shared.shapes;

import nl.gzmn.gZMNBuildtools.shared.noise.PerlinNoise;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Procedural trees. Logs (pattern 0) carry their axis so log blocks line up
 * with trunks and branches; leaves (pattern 1) never overwrite logs.
 */
public final class Trees {

    public static final int LOG = 0;
    public static final int LEAVES = 1;

    /** Collects blocks, letting logs win over leaves at the same spot. */
    private static final class Canvas {
        private final Map<Long, int[]> blocks = new HashMap<>();

        void log(double x, double y, double z, char axis) {
            int bx = (int) Math.round(x);
            int by = (int) Math.round(y);
            int bz = (int) Math.round(z);
            blocks.put(key(bx, by, bz), new int[]{bx, by, bz, LOG, axis});
        }

        void leaf(int x, int y, int z) {
            blocks.putIfAbsent(key(x, y, z), new int[]{x, y, z, LEAVES, 0});
        }

        void emit(PartSink sink) {
            blocks.values().forEach(b -> sink.accept(b[0], b[1], b[2], b[3], (char) b[4]));
        }

        private static long key(int x, int y, int z) {
            return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
        }
    }

    private Trees() {
    }

    /**
     * Grows a tree whose trunk starts at (x, y, z).
     *
     * @param height    trunk height
     * @param thickness trunk radius in blocks (1 = a single column)
     * @param leafSize  radius of leaf clusters
     */
    public static void generate(TreeStyle style, int x, int y, int z, int height, int thickness, int leafSize,
                                long seed, PartSink sink) {
        Random random = new Random(seed);
        Canvas canvas = new Canvas();
        PerlinNoise noise = new PerlinNoise(seed);
        switch (style) {
            case OAK -> branching(canvas, random, noise, x, y, z, height, thickness, leafSize, 4 + random.nextInt(3),
                    0.45, 0.4, true, false);
            case DEAD -> branching(canvas, random, noise, x, y, z, height, thickness, 0, 3 + random.nextInt(3),
                    0.4, 0.35, true, false);
            case WILLOW -> branching(canvas, random, noise, x, y, z, height, thickness, leafSize,
                    5 + random.nextInt(3), 0.55, 0.3, false, true);
            case BIRCH -> birch(canvas, random, noise, x, y, z, height, thickness, leafSize);
            case SPRUCE -> spruce(canvas, random, x, y, z, height, thickness, leafSize);
            case PALM -> palm(canvas, random, x, y, z, height, thickness, leafSize);
        }
        canvas.emit(sink);
    }

    /** A trunk with gentle lean; returns the top position. */
    private static double[] trunk(Canvas canvas, Random random, int x, int y, int z, int height, int thickness,
                                  double lean) {
        double angle = random.nextDouble() * Math.PI * 2;
        double lx = Math.cos(angle) * lean;
        double lz = Math.sin(angle) * lean;
        double cx = x;
        double cz = z;
        for (int i = 0; i < height; i++) {
            double t = i / (double) Math.max(1, height - 1);
            cx = x + lx * t * t * height;
            cz = z + lz * t * t * height;
            double r = Math.max(0, (thickness - 1) * (1 - 0.5 * t));
            disc(canvas, cx, y + i, cz, r);
        }
        return new double[]{cx, y + height - 1, cz};
    }

    private static void disc(Canvas canvas, double cx, int y, double cz, double r) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz <= r * r + 0.5) {
                    canvas.log(cx + dx, y, cz + dz, 'y');
                }
            }
        }
    }

    /** A straight log line from a to b with the axis of its main direction. */
    private static void limb(Canvas canvas, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        char axis = Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz) ? 'y'
                : Math.abs(dx) >= Math.abs(dz) ? 'x' : 'z';
        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            canvas.log(ax + dx * t, ay + dy * t, az + dz * t, axis);
        }
    }

    /** A lumpy ball of leaves. */
    private static void cluster(Canvas canvas, PerlinNoise noise, double cx, double cy, double cz, double radius,
                                double squash) {
        int r = (int) Math.ceil(radius + 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = (int) Math.round(cx) + dx;
                    int y = (int) Math.round(cy) + dy;
                    int z = (int) Math.round(cz) + dz;
                    double ey = (y - cy) / squash;
                    double d = Math.sqrt((x - cx) * (x - cx) + ey * ey + (z - cz) * (z - cz));
                    double lump = 1 + 0.35 * noise.noise(x * 0.35, y * 0.35, z * 0.35);
                    if (d <= radius * lump) {
                        canvas.leaf(x, y, z);
                    }
                }
            }
        }
    }

    private static void branching(Canvas canvas, Random random, PerlinNoise noise, int x, int y, int z, int height,
                                  int thickness, int leafSize, int branches, double branchLength, double lean,
                                  boolean recurse, boolean droop) {
        double[] top = trunk(canvas, random, x, y, z, height, thickness, lean * 0.15);
        double startAngle = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < branches; i++) {
            double h = y + height * (0.5 + 0.45 * i / Math.max(1, branches - 1));
            double t = (h - y) / height;
            double bx = x + (top[0] - x) * t * t;
            double bz = z + (top[2] - z) * t * t;
            double yaw = startAngle + i * 2.4 + random.nextGaussian() * 0.3;
            double pitch = Math.toRadians(25 + random.nextInt(30));
            double length = height * branchLength * (0.7 + 0.5 * random.nextDouble());
            double ex = bx + Math.cos(yaw) * Math.cos(pitch) * length;
            double ey = h + Math.sin(pitch) * length;
            double ez = bz + Math.sin(yaw) * Math.cos(pitch) * length;
            limb(canvas, bx, h, bz, ex, ey, ez);
            if (recurse && length > 4) {
                double yaw2 = yaw + (random.nextBoolean() ? 0.7 : -0.7);
                double l2 = length * 0.5;
                double fx = ex + Math.cos(yaw2) * l2 * 0.8;
                double fy = ey + l2 * 0.5;
                double fz = ez + Math.sin(yaw2) * l2 * 0.8;
                limb(canvas, ex, ey, ez, fx, fy, fz);
                if (leafSize > 0) {
                    cluster(canvas, noise, fx, fy, fz, leafSize * 0.8, 0.75);
                }
            }
            if (leafSize > 0) {
                cluster(canvas, noise, ex, ey, ez, leafSize, droop ? 0.6 : 0.75);
                if (droop) {
                    hangingStrands(canvas, random, ex, ey, ez, leafSize, height);
                }
            }
        }
        if (leafSize > 0) {
            cluster(canvas, noise, top[0], top[1] + 1, top[2], leafSize + 0.5, 0.8);
            if (droop) {
                hangingStrands(canvas, random, top[0], top[1], top[2], leafSize + 1, height);
            }
        }
    }

    /** Willow curtains: leaves hanging down from a cluster's rim. */
    private static void hangingStrands(Canvas canvas, Random random, double cx, double cy, double cz, double radius,
                                       int height) {
        int strands = (int) (radius * 4);
        for (int i = 0; i < strands; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            int sx = (int) Math.round(cx + Math.cos(a) * radius);
            int sz = (int) Math.round(cz + Math.sin(a) * radius);
            int length = 2 + random.nextInt(Math.max(2, height / 2));
            for (int dy = 0; dy < length; dy++) {
                canvas.leaf(sx, (int) Math.round(cy) - dy, sz);
            }
        }
    }

    private static void birch(Canvas canvas, Random random, PerlinNoise noise, int x, int y, int z, int height,
                              int thickness, int leafSize) {
        double[] top = trunk(canvas, random, x, y, z, height, thickness, 0.02);
        // A tall, narrow crown over the top third.
        double crownBottom = y + height * 0.6;
        for (double cy = crownBottom; cy <= top[1] + 2; cy += 1.5) {
            double t = (cy - crownBottom) / (top[1] + 2 - crownBottom);
            double r = leafSize * (0.6 + 0.6 * Math.sin(Math.PI * Math.min(1, t * 0.9 + 0.1)));
            cluster(canvas, noise, top[0], cy, top[2], r, 0.9);
        }
    }

    private static void spruce(Canvas canvas, Random random, int x, int y, int z, int height, int thickness,
                               int leafSize) {
        double[] top = trunk(canvas, random, x, y, z, height, thickness, 0);
        int start = y + Math.max(1, height / 4);
        int end = (int) top[1] + 2;
        double maxRadius = leafSize + 1;
        for (int ly = start; ly <= end; ly++) {
            double t = (ly - start) / (double) Math.max(1, end - start);
            // Layered tiers: each tier flares out and shrinks again.
            double tier = 1 - ((ly - start) % 3) * 0.25;
            double r = Math.max(0.6, maxRadius * (1 - t) * tier + (ly == end ? 0 : 0.5));
            int ir = (int) Math.ceil(r);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        canvas.leaf(x + dx, ly, z + dz);
                    }
                }
            }
        }
    }

    private static void palm(Canvas canvas, Random random, int x, int y, int z, int height, int thickness,
                             int leafSize) {
        double[] top = trunk(canvas, random, x, y, z, height, thickness, 0.35);
        int fronds = 6 + random.nextInt(3);
        double length = leafSize * 2.0 + 1;
        for (int f = 0; f < fronds; f++) {
            double yaw = f * Math.PI * 2 / fronds + random.nextGaussian() * 0.15;
            int steps = (int) Math.ceil(length * 2);
            for (int i = 0; i <= steps; i++) {
                double d = length * i / steps;
                // Rise a little, then droop: a parabola along the frond.
                double fy = top[1] + 1 + 0.6 * d - 0.12 * d * d;
                int fx = (int) Math.round(top[0] + Math.cos(yaw) * d);
                int fz = (int) Math.round(top[2] + Math.sin(yaw) * d);
                canvas.leaf(fx, (int) Math.round(fy), fz);
            }
        }
        canvas.leaf((int) Math.round(top[0]), (int) top[1] + 1, (int) Math.round(top[2]));
    }
}
