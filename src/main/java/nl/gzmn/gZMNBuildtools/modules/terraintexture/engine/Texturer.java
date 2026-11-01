package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BlockSource;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.PositionHash;
import nl.gzmn.gZMNBuildtools.shared.noise.PerlinNoise;
import nl.gzmn.gZMNBuildtools.shared.terrain.TerrainMath;
import nl.gzmn.gZMNBuildtools.shared.terrain.TerrainView;

import java.util.function.Predicate;

/**
 * Repaints terrain with a preset: for each block it works out how deep it is
 * below the surface, how steep the slope is there, its height and a smooth
 * noise value, and lets the preset's first matching rule pick the block.
 */
public final class Texturer {

    /** Receives each change; a null block means air (a plant cleared off new ground). */
    @FunctionalInterface
    public interface Writer {
        void write(int x, int y, int z, String block);
    }

    /**
     * Plants that only survive on soil, so grass tufts and flowers don't end up
     * floating on new sand, stone or snow.
     *
     * @param needsSoil blocks removed when the ground under them stops being soil
     * @param soil      ground they can stay on
     */
    public record Plants(Predicate<String> needsSoil, Predicate<String> soil) {
        public static final Plants KEEP = new Plants(id -> false, id -> true);
    }

    /** Which positions to texture (a selection, or a brush sphere). */
    @FunctionalInterface
    public interface Inside {
        boolean test(int x, int y, int z);
    }

    private static final double NOISE_SCALE = 0.07;

    private final TexturePreset preset;
    private final Predicate<String> solid;
    private final Predicate<String> paintable;
    private final Plants plants;
    private final long seed;
    private final PerlinNoise noise;

    /**
     * @param solid     whether a block counts as ground (for depth and slope)
     * @param paintable whether a block may be repainted (natural terrain by default)
     */
    public Texturer(TexturePreset preset, Predicate<String> solid, Predicate<String> paintable, Plants plants,
                    long seed) {
        this.preset = preset;
        this.solid = solid;
        this.paintable = paintable;
        this.plants = plants;
        this.seed = seed;
        this.noise = new PerlinNoise(seed);
    }

    /** @return the number of blocks changed */
    public int apply(BlockSource source, Bounds region, Inside inside, Writer writer) {
        TerrainView view = new TerrainView() {
            @Override
            public boolean isSolid(int x, int y, int z) {
                String id = source.blockAt(x, y, z);
                return id != null && solid.test(id);
            }

            @Override
            public int minY() {
                return source.minY();
            }

            @Override
            public int maxY() {
                return source.maxY();
            }
        };
        int maxDepth = preset.maxDepth();
        int changed = 0;
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int z = region.minZ(); z <= region.maxZ(); z++) {
                // Count the solid run reaching down into the region from above it.
                int top = Math.min(source.maxY(), region.maxY());
                int depth = -1;
                for (int y = Math.min(source.maxY(), top + maxDepth + 1); y > top; y--) {
                    depth = view.isSolid(x, y, z) ? depth + 1 : -1;
                }
                int surface = Integer.MIN_VALUE;
                double slope = 0;
                double patch = 0;
                for (int y = top; y >= Math.max(source.minY(), region.minY()); y--) {
                    String id = source.blockAt(x, y, z);
                    if (id == null || !solid.test(id)) {
                        depth = -1;
                        continue;
                    }
                    depth++;
                    if (depth == 0 || surface == Integer.MIN_VALUE) {
                        surface = y + depth;
                        slope = TerrainMath.slopeDegrees(view, x, surface, z, 2);
                        double n = noise.noise(x * NOISE_SCALE, surface * NOISE_SCALE, z * NOISE_SCALE) * 0.5 + 0.5;
                        patch = Math.max(0, Math.min(1, n));
                    }
                    if (depth > maxDepth || !inside.test(x, y, z) || !paintable.test(id)) {
                        continue;
                    }
                    TextureRule rule = preset.match(depth, slope, y, patch);
                    if (rule == null) {
                        continue;
                    }
                    String block = rule.choose(y, PositionHash.hash01(x, y, z, seed));
                    if (!block.equals(id)) {
                        writer.write(x, y, z, block);
                        changed++;
                        if (depth == 0 && !plants.soil().test(block)) {
                            changed += clearPlants(source, x, y + 1, z, writer);
                        }
                    }
                }
            }
        }
        return changed;
    }

    private int clearPlants(BlockSource source, int x, int y, int z, Writer writer) {
        int cleared = 0;
        for (int yy = y; yy <= source.maxY(); yy++) {
            String above = source.blockAt(x, yy, z);
            if (above == null || !plants.needsSoil().test(above)) {
                break;
            }
            writer.write(x, yy, z, null);
            cleared++;
        }
        return cleared;
    }
}
