package nl.gzmn.gZMNBuildtools.modules.loft.engine;

import nl.gzmn.gZMNBuildtools.shared.shapes.Splines;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Lofts a surface along a smooth curve through stations (points with a frame
 * size): every station gets an elliptical cross-section perpendicular to the
 * curve, and the sections are placed as an outline ring or a filled disc.
 * Pure: no world access, deterministic for fixed inputs.
 */
public final class LoftPlanner {

    /** One loft point: its position and the frame (cross-section size) there. */
    public record Station(double x, double y, double z, double width, double height) {
    }

    /** One block position of the loft. */
    public record Cell(int x, int y, int z) {
    }

    /** The loft as cells, or why there is none. */
    public record Plan(Set<Cell> cells, int blockCount, String problem) {
    }

    /** The top solid block of a column, or -1 when there is none. */
    @FunctionalInterface
    public interface ColumnGround {
        int ground(int x, int z);
    }

    private LoftPlanner() {
    }

    public static Plan plan(List<Station> stations, LoftOptions options, ColumnGround ground) {
        if (stations.size() < 2) {
            return new Plan(Set.of(), 0, "Add at least two loft points first.");
        }
        List<int[]> points = new ArrayList<>();
        for (Station s : stations) {
            points.add(new int[]{(int) Math.round(s.x()), (int) Math.round(s.y()), (int) Math.round(s.z())});
        }
        double density = options.lowPoly() ? 1.5 : options.density();
        List<double[]> samples = Splines.sample(points, density);
        if (samples.size() < 2) {
            return new Plan(Set.of(), 0, "Add at least two loft points first.");
        }

        Set<Cell> cells = new LinkedHashSet<>();
        for (int i = 0; i < samples.size(); i++) {
            double[] c = samples.get(i);
            double[] tangent = tangent(samples, i);
            double[] up = Math.abs(tangent[1]) > 0.9 ? new double[]{1, 0, 0} : new double[]{0, 1, 0};
            double[] e1 = normalize(cross(tangent, up));
            double[] e2 = normalize(cross(tangent, e1));
            double w = Math.max(1, frameAt(stations, samples, i, true, options.smooth()));
            double h = Math.max(1, frameAt(stations, samples, i, false, options.smooth()));
            int segments = options.lowPoly() ? 8
                    : Math.max(12, Math.min(48, (int) Math.round(Math.PI * (w + h) / 2)));
            if (options.fill()) {
                int ri = (int) Math.ceil(w / 2);
                int rj = (int) Math.ceil(h / 2);
                for (int gi = -ri; gi <= ri; gi++) {
                    for (int gj = -rj; gj <= rj; gj++) {
                        double u = gi / (w / 2);
                        double v = gj / (h / 2);
                        if (u * u + v * v <= 1) {
                            cells.add(cellAt(c, e1, e2, gi, gj));
                        }
                    }
                }
            } else {
                for (int k = 0; k < segments; k++) {
                    double a = 2 * Math.PI * k / segments;
                    cells.add(cellAt(c, e1, e2, Math.cos(a) * w / 2, Math.sin(a) * h / 2));
                }
            }
        }

        if (options.stackDown()) {
            Map<Long, Integer> lowest = new HashMap<>();
            for (Cell cell : cells) {
                long key = (((long) cell.x()) << 32) | (cell.z() & 0xffffffffL);
                lowest.merge(key, cell.y(), Math::min);
            }
            for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
                int x = (int) (e.getKey() >> 32);
                int z = (int) e.getKey();
                int g = ground.ground(x, z);
                if (g >= 0) {
                    for (int y = g + 1; y < e.getValue(); y++) {
                        cells.add(new Cell(x, y, z));
                    }
                }
            }
        }
        return new Plan(cells, cells.size(), null);
    }

    private static Cell cellAt(double[] c, double[] e1, double[] e2, double o1, double o2) {
        return new Cell((int) Math.round(c[0] + e1[0] * o1 + e2[0] * o2),
                (int) Math.round(c[1] + e1[1] * o1 + e2[1] * o2),
                (int) Math.round(c[2] + e1[2] * o1 + e2[2] * o2));
    }

    /** The frame size at sample i: blended between the segment's stations when smooth, else the segment's start. */
    private static double frameAt(List<Station> stations, List<double[]> samples, int i, boolean width,
                                  boolean smooth) {
        double[] s = samples.get(i);
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        double bestU = 0;
        for (int seg = 0; seg < stations.size() - 1; seg++) {
            Station a = stations.get(seg);
            Station b = stations.get(seg + 1);
            double abx = b.x() - a.x();
            double aby = b.y() - a.y();
            double abz = b.z() - a.z();
            double len2 = abx * abx + aby * aby + abz * abz;
            double u = len2 < 1e-9 ? 0 : ((s[0] - a.x()) * abx + (s[1] - a.y()) * aby + (s[2] - a.z()) * abz) / len2;
            u = Math.max(0, Math.min(1, u));
            double px = a.x() + abx * u;
            double py = a.y() + aby * u;
            double pz = a.z() + abz * u;
            double d = (s[0] - px) * (s[0] - px) + (s[1] - py) * (s[1] - py) + (s[2] - pz) * (s[2] - pz);
            if (d < bestDist) {
                bestDist = d;
                best = seg;
                bestU = u;
            }
        }
        Station a = stations.get(best);
        Station b = stations.get(best + 1);
        double va = width ? a.width() : a.height();
        double vb = width ? b.width() : b.height();
        return smooth ? va + (vb - va) * bestU : va;
    }

    private static double[] tangent(List<double[]> samples, int i) {
        double[] a = samples.get(Math.max(0, i - 1));
        double[] b = samples.get(Math.min(samples.size() - 1, i + 1));
        return normalize(new double[]{b[0] - a[0], b[1] - a[1], b[2] - a[2]});
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1e-9) {
            return new double[]{0, 1, 0};
        }
        return new double[]{v[0] / len, v[1] / len, v[2] / len};
    }
}
