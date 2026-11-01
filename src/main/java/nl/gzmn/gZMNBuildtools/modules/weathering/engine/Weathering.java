package nl.gzmn.gZMNBuildtools.modules.weathering.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BlockCatalog;
import nl.gzmn.gZMNBuildtools.shared.build.BlockSource;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.LongHashSet;
import nl.gzmn.gZMNBuildtools.shared.build.PositionHash;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import nl.gzmn.gZMNBuildtools.shared.noise.PerlinNoise;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ages a build the way weather would: moss creeping up from the ground in
 * patches, cracks around the middle, worn-away corners and edges (more near the
 * top), and vines hanging from the walls. Every decision is a deterministic
 * function of the position and the build, so strokes over the same spot agree.
 */
public final class Weathering {

    /** Where a vine goes and which side of it the wall is on (north, south, east, west). */
    @FunctionalInterface
    public interface VineSink {
        void accept(int x, int y, int z, String wallSide);
    }

    private static final int[][] SIDES = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
    /** The side of the vine block the wall is on, for each entry of {@link #SIDES}. */
    private static final String[] WALL_SIDE_OF_VINE = {"south", "north", "west", "east"};

    private final BlockCatalog catalog;

    public Weathering(BlockCatalog catalog) {
        this.catalog = catalog;
    }

    /** One pass over a build: the member blocks it changes, and the vines it hangs. */
    public final class Pass {
        private final BuildScan scan;
        private final BlockSource source;
        private final WeatherOptions options;
        private final long seed;
        private final PerlinNoise noise;
        private final Set<Long> removed = ConcurrentHashMap.newKeySet();

        Pass(BuildScan scan, BlockSource source, WeatherOptions options) {
            this.scan = scan;
            this.source = source;
            this.options = options;
            this.seed = PositionHash.seedFor(scan.bounds());
            this.noise = new PerlinNoise(seed);
        }

        /** 0 at the bottom of the build, 1 at the top. */
        private double height(int y) {
            Bounds b = scan.bounds();
            return b.sizeY() <= 1 ? 0 : (y - b.minY()) / (double) (b.sizeY() - 1);
        }

        /** Smooth 0..1 patches, so moss and cracks come in clusters rather than speckles. */
        private double patch(int x, int y, int z, int layer) {
            double v = noise.noise(x * 0.18 + layer * 31.7, y * 0.18, z * 0.18) * 0.5 + 0.5;
            return Math.max(0, Math.min(1, v));
        }

        private double roll(int x, int y, int z, int salt) {
            return PositionHash.hash01(x, y, z, seed ^ (salt * 0x9E3779B97F4A7C15L));
        }

        private boolean air(int x, int y, int z) {
            return source.blockAt(x, y, z) == null;
        }

        private int exposedFaces(int x, int y, int z) {
            int n = air(x, y + 1, z) ? 1 : 0;
            n += air(x, y - 1, z) ? 1 : 0;
            for (int[] s : SIDES) {
                n += air(x + s[0], y, z + s[2]) ? 1 : 0;
            }
            return n;
        }

        /** The block's texture form, keeping its shape, if the game has it. */
        private Optional<String> textured(BlockCatalog.Info info, String prefix) {
            return catalog.withTexture(info.group(), prefix).flatMap(base -> catalog.variant(base, info.kind()));
        }

        public BuildPainter.Chooser chooser() {
            return (x, y, z, info) -> {
                if (info.kind() == ShapeKind.OTHER) {
                    return null;
                }
                double s = options.strength();
                double h = height(y);
                double patch = patch(x, y, z, 0);

                // Worn edges: corners and rims lose blocks or crumble to slabs, more towards the top.
                if (exposedFaces(x, y, z) >= 2) {
                    double decay = s * options.decay() * 0.3 * (0.35 + 0.65 * h);
                    if (roll(x, y, z, 1) < decay) {
                        Optional<String> slab = catalog.variant(info.base() == null ? info.id() : info.base(),
                                ShapeKind.SLAB);
                        if (info.kind() == ShapeKind.FULL && slab.isPresent() && roll(x, y, z, 2) < 0.5) {
                            return slab.get();
                        }
                        removed.add(LongHashSet.pack(x, y, z));
                        return "air";
                    }
                }
                String texture = catalog.textureOf(info.base() == null ? info.id() : info.base());
                // Moss creeps up from the ground in patches.
                double moss = s * options.moss() * 0.9 * Math.pow(1 - h, 3) * (0.3 + 0.7 * patch);
                if (!texture.equals("mossy_") && roll(x, y, z, 3) < moss) {
                    Optional<String> mossy = textured(info, "mossy_");
                    if (mossy.isPresent()) {
                        return mossy.get();
                    }
                }
                // Cracks cluster around the middle, away from the moss.
                double crack = s * options.cracks() * 0.6 * Math.max(0, 1 - Math.abs(h - 0.45) * 1.4)
                        * (0.3 + 0.7 * (1 - patch));
                if (texture.isEmpty() && roll(x, y, z, 4) < crack) {
                    Optional<String> cracked = textured(info, "cracked_");
                    if (cracked.isPresent()) {
                        return cracked.get();
                    }
                }
                return null;
            };
        }

        /**
         * Vines hanging from open wall faces covered by {@code stroke}; call
         * after the chooser has run so vines skip walls that crumbled away.
         */
        public void vines(BuildPainter.StrokeFilter stroke, VineSink sink) {
            if (options.vines() <= 0) {
                return;
            }
            Set<Long> taken = new java.util.HashSet<>();
            scan.members().forEach(key -> {
                int x = LongHashSet.unpackX(key);
                int y = LongHashSet.unpackY(key);
                int z = LongHashSet.unpackZ(key);
                if (!stroke.test(x, y, z) || removed.contains(key)) {
                    return;
                }
                double h = height(y);
                for (int i = 0; i < SIDES.length; i++) {
                    int vx = x + SIDES[i][0];
                    int vz = z + SIDES[i][2];
                    if (!air(vx, y, vz)) {
                        continue;
                    }
                    double chance = options.strength() * options.vines() * 0.07 * (0.3 + 0.7 * h)
                            * (0.3 + 0.7 * patch(x, y, z, 1));
                    if (roll(x, y, z, 10 + i) >= chance) {
                        continue;
                    }
                    int length = 1 + (int) (roll(x, y, z, 20 + i) * (2 + 5 * h));
                    for (int d = 0; d < length; d++) {
                        int vy = y - d;
                        if (vy <= source.minY() || !air(vx, vy, vz) || !taken.add(LongHashSet.pack(vx, vy, vz))) {
                            break;
                        }
                        sink.accept(vx, vy, vz, WALL_SIDE_OF_VINE[i]);
                    }
                }
            });
        }
    }

    public Pass pass(BuildScan scan, BlockSource source, WeatherOptions options) {
        return new Pass(scan, source, options);
    }
}
