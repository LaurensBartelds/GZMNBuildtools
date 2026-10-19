package nl.gzmn.gZMNBuildtools.brush;

import nl.gzmn.gZMNBuildtools.noise.PerlinNoise;

/**
 * Computes, for every column of a {@link HeightField}, how many blocks its
 * surface should move (positive = up). Columns outside the circular brush or
 * without a surface get 0.
 */
@FunctionalInterface
public interface HeightOperation {

    int[] deltas(HeightField field);

    /**
     * Smooth falloff weight in [0, 1] for normalised distance {@code t}: 1 up
     * to {@code hardness}, then easing to 0 at t = 1.
     */
    static double falloff(double t, double hardness) {
        if (t >= 1) {
            return 0;
        }
        double h = Math.max(0, Math.min(0.99, hardness));
        double u = Math.max(0, (t - h) / (1 - h));
        double v = 1 - u * u;
        return v * v;
    }

    /** Builds a delta array from a per-column target-shift function. */
    static int[] perColumn(HeightField field, ColumnFunction function) {
        int r = field.radius();
        int[] out = new int[field.size() * field.size()];
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                if (!field.hasSurface(dx, dz)) {
                    continue;
                }
                double t = Math.sqrt(dx * dx + dz * dz) / (r + 0.5);
                if (t >= 1) {
                    continue;
                }
                out[field.index(dx, dz)] = (int) Math.round(function.shift(dx, dz, field.height(dx, dz), t));
            }
        }
        return out;
    }

    @FunctionalInterface
    interface ColumnFunction {
        double shift(int dx, int dz, int height, double t);
    }

    /** Raises (or with negative amount, lowers) terrain by up to {@code amount} blocks. */
    static HeightOperation raise(double amount, double hardness) {
        return field -> perColumn(field, (dx, dz, h, t) -> amount * falloff(t, hardness));
    }

    /** Pulls terrain towards the height of the centre column. */
    static HeightOperation flatten(double strength, double hardness) {
        return field -> {
            int target = field.centerHeight();
            return perColumn(field, (dx, dz, h, t) -> (target - h) * strength * falloff(t, hardness));
        };
    }

    /** Pulls terrain towards a Gaussian-weighted average of its neighbourhood. */
    static HeightOperation smooth(int kernel, int iterations, double strength) {
        return field -> {
            int r = field.radius();
            int size = field.size();
            double[] current = new double[size * size];
            boolean[] valid = new boolean[size * size];
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    int i = field.index(dx, dz);
                    valid[i] = field.hasSurface(dx, dz);
                    current[i] = valid[i] ? field.height(dx, dz) : 0;
                }
            }
            double sigma = Math.max(0.5, kernel / 2.0);
            for (int pass = 0; pass < iterations; pass++) {
                double[] next = current.clone();
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int i = field.index(dx, dz);
                        if (!valid[i]) {
                            continue;
                        }
                        double sum = 0;
                        double weights = 0;
                        for (int kz = -kernel; kz <= kernel; kz++) {
                            for (int kx = -kernel; kx <= kernel; kx++) {
                                int nx = dx + kx;
                                int nz = dz + kz;
                                if (Math.abs(nx) > r || Math.abs(nz) > r || !valid[field.index(nx, nz)]) {
                                    continue;
                                }
                                double w = Math.exp(-(kx * kx + kz * kz) / (2 * sigma * sigma));
                                sum += current[field.index(nx, nz)] * w;
                                weights += w;
                            }
                        }
                        next[i] = sum / weights;
                    }
                }
                current = next;
            }
            final double[] smoothed = current;
            return perColumn(field, (dx, dz, h, t) ->
                    (smoothed[field.index(dx, dz)] - h) * strength * falloff(t, 0.3));
        };
    }

    /** Snaps heights to steps of {@code step} blocks, blending out towards the edge. */
    static HeightOperation terrace(int step, int offset, double hardness) {
        return field -> perColumn(field, (dx, dz, h, t) -> {
            double target = Math.round((h - offset) / (double) step) * step + offset;
            return (target - h) * falloff(t, hardness);
        });
    }

    /** Displaces terrain by fractal noise of up to {@code amplitude} blocks. */
    static HeightOperation roughen(double amplitude, double scale, long seed) {
        PerlinNoise noise = new PerlinNoise(seed);
        return field -> perColumn(field, (dx, dz, h, t) -> {
            double x = (field.centerX() + dx) / scale;
            double z = (field.centerZ() + dz) / scale;
            double n = Math.max(-1, Math.min(1, noise.fractalNoise(x, 0.5, z, 3, 0.5, 2.0) * 1.5));
            return amplitude * n * falloff(t, 0.2);
        });
    }
}
