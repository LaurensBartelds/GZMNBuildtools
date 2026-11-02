package nl.gzmn.gZMNBuildtools.shared.shapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smooth curves through any number of points (centripetal Catmull-Rom, which
 * passes through every point without loops or overshoot) and the shapes built
 * along them: tubes, walls and roads.
 */
public final class Splines {

    /** Road part: the surface layer. */
    public static final int ROAD_SURFACE = 0;
    /** Road part: support filled in below the surface. */
    public static final int ROAD_FILL = 1;
    /** Road part: space cleared above the road. */
    public static final int ROAD_CLEAR = 2;

    /** River part: the bed under the water (the river's pattern). */
    public static final int RIVER_BED = 0;
    /** River part: water. */
    public static final int RIVER_WATER = 1;
    /** River part: ground cut away above the water to form the valley. */
    public static final int RIVER_CLEAR = 2;
    /** River part: a bank to hold the water in, only needed where there is no ground. */
    public static final int RIVER_BANK = 3;

    private Splines() {
    }

    /** Points along the curve roughly {@code 1 / density} blocks apart, starting and ending at the ends. */
    public static List<double[]> sample(List<int[]> points, double density) {
        List<double[]> out = new ArrayList<>();
        if (points.isEmpty()) {
            return out;
        }
        List<double[]> p = new ArrayList<>();
        for (int[] point : points) {
            double[] d = {point[0], point[1], point[2]};
            if (p.isEmpty() || dist(p.get(p.size() - 1), d) > 1e-9) {
                p.add(d);
            }
        }
        if (p.size() == 1) {
            out.add(p.get(0));
            return out;
        }
        for (int i = 0; i < p.size() - 1; i++) {
            double[] p1 = p.get(i);
            double[] p2 = p.get(i + 1);
            double[] p0 = i > 0 ? p.get(i - 1) : extrapolate(p1, p2);
            double[] p3 = i + 2 < p.size() ? p.get(i + 2) : extrapolate(p2, p1);
            int steps = Math.max(1, (int) Math.ceil(dist(p1, p2) * density));
            for (int s = 0; s < steps; s++) {
                out.add(centripetal(p0, p1, p2, p3, s / (double) steps));
            }
        }
        out.add(p.get(p.size() - 1));
        return out;
    }

    private static double[] extrapolate(double[] a, double[] b) {
        return new double[]{2 * a[0] - b[0], 2 * a[1] - b[1], 2 * a[2] - b[2]};
    }

    private static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Centripetal Catmull-Rom point at fraction u (0..1) between p1 and p2. */
    static double[] centripetal(double[] p0, double[] p1, double[] p2, double[] p3, double u) {
        double t0 = 0;
        double t1 = t0 + Math.max(1e-6, Math.sqrt(dist(p0, p1)));
        double t2 = t1 + Math.max(1e-6, Math.sqrt(dist(p1, p2)));
        double t3 = t2 + Math.max(1e-6, Math.sqrt(dist(p2, p3)));
        double t = t1 + (t2 - t1) * u;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            double a1 = (t1 - t) / (t1 - t0) * p0[k] + (t - t0) / (t1 - t0) * p1[k];
            double a2 = (t2 - t) / (t2 - t1) * p1[k] + (t - t1) / (t2 - t1) * p2[k];
            double a3 = (t3 - t) / (t3 - t2) * p2[k] + (t - t2) / (t3 - t2) * p3[k];
            double b1 = (t2 - t) / (t2 - t0) * a1 + (t - t0) / (t2 - t0) * a2;
            double b2 = (t3 - t) / (t3 - t1) * a2 + (t - t1) / (t3 - t1) * a3;
            out[k] = (t2 - t) / (t2 - t1) * b1 + (t - t1) / (t2 - t1) * b2;
        }
        return out;
    }

    /** A tube of the given radius along the curve; hollow tubes keep a one-block shell. */
    public static void tube(List<double[]> samples, double radius, boolean hollow, BlockSink sink) {
        Set<Long> solid = stamp(samples, radius);
        Set<Long> inner = hollow && radius > 1 ? stamp(samples, radius - 1) : Set.of();
        for (long key : solid) {
            if (!inner.contains(key)) {
                sink.accept(unpackX(key), unpackY(key), unpackZ(key));
            }
        }
    }

    private static Set<Long> stamp(List<double[]> samples, double radius) {
        Set<Long> out = new HashSet<>();
        int r = (int) Math.ceil(radius);
        double r2 = radius * radius;
        for (double[] s : samples) {
            int cx = (int) Math.round(s[0]);
            int cy = (int) Math.round(s[1]);
            int cz = (int) Math.round(s[2]);
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        double dx = cx + x - s[0];
                        double dy = cy + y - s[1];
                        double dz = cz + z - s[2];
                        if (dx * dx + dy * dy + dz * dz <= r2 + 0.25) {
                            out.add(pack(cx + x, cy + y, cz + z));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** A wall rising {@code height} blocks from the curve, {@code thickness} blocks across. */
    public static void wall(List<double[]> samples, int height, int thickness, BlockSink sink) {
        Set<Long> placed = new HashSet<>();
        across(samples, thickness, (x, y, z) -> {
            for (int dy = 0; dy < height; dy++) {
                if (placed.add(pack(x, y + dy, z))) {
                    sink.accept(x, y + dy, z);
                }
            }
        });
    }

    /**
     * A road {@code width} blocks wide whose surface follows the curve, with
     * {@code fill} blocks of support underneath and {@code clear} blocks of
     * air above (so it cuts through hills and bridges dips).
     */
    public static void road(List<double[]> samples, int width, int clear, int fill, PartSink sink) {
        Set<Long> surface = new HashSet<>();
        across(samples, width, (x, y, z) -> surface.add(pack(x, y, z)));
        Set<Long> done = new HashSet<>(surface);
        for (long key : surface) {
            int x = unpackX(key);
            int y = unpackY(key);
            int z = unpackZ(key);
            sink.accept(x, y, z, ROAD_SURFACE, (char) 0);
            for (int dy = 1; dy <= fill; dy++) {
                if (done.add(pack(x, y - dy, z))) {
                    sink.accept(x, y - dy, z, ROAD_FILL, (char) 0);
                }
            }
            for (int dy = 1; dy <= clear; dy++) {
                // Never clear a spot that is road surface higher up the slope.
                if (!surface.contains(pack(x, y + dy, z)) && done.add(pack(x, y + dy, z))) {
                    sink.accept(x, y + dy, z, ROAD_CLEAR, (char) 0);
                }
            }
        }
    }

    /** The shape of a river channel; see {@link #river}. */
    public record RiverShape(double width, int depth, int sink, int valley, double slope) {
    }

    /** What {@link #river} worked out about the flow. */
    public record RiverFlow(boolean reversed, int sourceLevel, int mouthLevel) {
    }

    /**
     * A river along the curve. It always flows downhill: when the curve ends
     * higher than it starts it is followed backwards, and the water level never
     * rises along the way (it steps down where the ground drops). The water
     * sits {@code sink} blocks below the curve in a rounded channel
     * {@code width} across and up to {@code depth} deep, with the bed under
     * it. Above the water, ground is cut away up to {@code valley} blocks high
     * with banks sloping {@code slope} blocks up per block out, so the river
     * carves a valley through hills; banks are emitted next to the channel for
     * the places where the ground falls away.
     */
    public static RiverFlow river(List<double[]> samples, RiverShape shape, PartSink sink) {
        if (samples.isEmpty()) {
            return new RiverFlow(false, 0, 0);
        }
        List<double[]> path = new ArrayList<>(samples);
        boolean reversed = path.get(path.size() - 1)[1] > path.get(0)[1];
        if (reversed) {
            Collections.reverse(path);
        }
        int[] level = new int[path.size()];
        int current = Integer.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            current = Math.min(current, (int) Math.round(path.get(i)[1]) - shape.sink());
            level[i] = current;
        }

        double half = shape.width() / 2.0;
        double ring = shape.slope() <= 0 ? 1.5 : Math.max(1.5, shape.valley() / shape.slope());
        double reach = half + ring;
        int r = (int) Math.ceil(reach) + 1;
        // Each column belongs to its nearest sample.
        Map<Long, double[]> owner = new HashMap<>();
        for (int i = 0; i < path.size(); i++) {
            double[] s = path.get(i);
            int cx = (int) Math.round(s[0]);
            int cz = (int) Math.round(s[2]);
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    double d = Math.hypot(x - s[0], z - s[2]);
                    if (d > reach) {
                        continue;
                    }
                    long key = pack(x, 0, z);
                    double[] best = owner.get(key);
                    if (best == null || d < best[0]) {
                        owner.put(key, new double[]{d, i});
                    }
                }
            }
        }

        for (Map.Entry<Long, double[]> entry : owner.entrySet()) {
            int x = unpackX(entry.getKey());
            int z = unpackZ(entry.getKey());
            double d = entry.getValue()[0];
            int water = level[(int) entry.getValue()[1]];
            int top = water + shape.valley();
            if (d <= half) {
                double t = half <= 0 ? 0 : d / half;
                int bed = water - Math.max(1, (int) Math.round(shape.depth() * (1 - t * t)));
                sink.accept(x, bed, z, RIVER_BED, (char) 0);
                for (int y = bed + 1; y <= water; y++) {
                    sink.accept(x, y, z, RIVER_WATER, (char) 0);
                }
                for (int y = water + 1; y <= top; y++) {
                    sink.accept(x, y, z, RIVER_CLEAR, (char) 0);
                }
            } else {
                double out = d - half;
                int from = water + 1 + (int) Math.ceil(out * Math.max(0, shape.slope()));
                for (int y = from; y <= top; y++) {
                    sink.accept(x, y, z, RIVER_CLEAR, (char) 0);
                }
                if (out <= 1.5) {
                    for (int y = water - shape.depth(); y <= water; y++) {
                        sink.accept(x, y, z, RIVER_BANK, (char) 0);
                    }
                }
            }
        }
        return new RiverFlow(reversed, level[0], level[level.length - 1]);
    }

    /** Visits the blocks across the curve: a horizontal line of {@code width} perpendicular to it. */
    private static void across(List<double[]> samples, int width, BlockSink visit) {
        double half = (width - 1) / 2.0;
        for (int i = 0; i < samples.size(); i++) {
            double[] s = samples.get(i);
            double[] a = samples.get(Math.max(0, i - 1));
            double[] b = samples.get(Math.min(samples.size() - 1, i + 1));
            double tx = b[0] - a[0];
            double tz = b[2] - a[2];
            double len = Math.sqrt(tx * tx + tz * tz);
            double px = len < 1e-9 ? 1 : -tz / len;
            double pz = len < 1e-9 ? 0 : tx / len;
            for (double o = -half; o <= half + 1e-9; o += 0.5) {
                visit.accept((int) Math.round(s[0] + px * o), (int) Math.round(s[1]), (int) Math.round(s[2] + pz * o));
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    private static int unpackX(long key) {
        return (int) (key >> 38);
    }

    private static int unpackY(long key) {
        return (int) (key << 52 >> 52);
    }

    private static int unpackZ(long key) {
        return (int) (key << 26 >> 38);
    }
}
