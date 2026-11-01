package nl.gzmn.gZMNBuildtools.generate;

import nl.gzmn.gZMNBuildtools.noise.PerlinNoise;

/**
 * Natural boulders: a noisy, slightly flattened ellipsoid sunk partly into
 * the ground so it looks embedded rather than dropped on top.
 */
public final class Rocks {

    private Rocks() {
    }

    /**
     * @param x,y,z     the ground block the rock sits on
     * @param flatness  0 = round, 0.8 = a flat slab
     * @param roughness how lumpy the surface is (0..1)
     * @param sink      fraction of the radius buried below the ground (0..0.8)
     */
    public static void generate(int x, int y, int z, double radius, double flatness, double roughness, double sink,
                                long seed, BlockSink out) {
        PerlinNoise noise = new PerlinNoise(seed);
        double vertical = radius * (1 - flatness);
        double cy = y + 1 + vertical * (1 - 2 * sink);
        int r = (int) Math.ceil(radius * (1 + roughness));
        int rv = (int) Math.ceil(vertical * (1 + roughness));
        double scale = 1.6 / Math.max(1, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -rv - 1; dy <= rv + 1; dy++) {
                    int by = (int) Math.round(cy) + dy;
                    double ny = (by - cy) / Math.max(0.5, vertical);
                    double nx = dx / radius;
                    double nz = dz / radius;
                    double d = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    double lump = 1 + roughness * 0.5 * noise.fractalNoise((x + dx) * scale, by * scale,
                            (z + dz) * scale, 3, 0.5, 2.0);
                    if (d <= lump) {
                        out.accept(x + dx, by, z + dz);
                    }
                }
            }
        }
    }
}
