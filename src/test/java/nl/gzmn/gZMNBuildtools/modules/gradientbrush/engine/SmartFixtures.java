package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockFilters;
import nl.gzmn.gZMNBuildtools.shared.palette.PaletteGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Shared test world and catalogue built from the bundled block list. */
final class SmartFixtures {

    static final BlockColors COLORS = BlockColors.bundled();

    /** Name-based full-cube guess, standing in for WorldEdit's block material data. */
    static final Predicate<String> FULL_CUBE = id -> !BlockFilters.looksPartial(id)
            && !id.contains("door") && !id.contains("torch") && !id.contains("flower") && !id.equals("short_grass");

    static final Set<String> DETAILS = Set.of("glass", "glowstone", "sea_lantern", "chest", "oak_leaves", "water");

    static final BlockCatalog CATALOG = new BlockCatalog(COLORS.ids(), FULL_CUBE);
    static final BlockRules RULES = new BlockRules(CATALOG, DETAILS::contains);
    static final VariantResolver VARIANTS = new VariantResolver(CATALOG, COLORS);
    static final PaletteGenerator GENERATOR = new PaletteGenerator(COLORS,
            BlockFilters.anyGlob(BlockFilters.DEFAULT_PALETTE_EXCLUDES).negate().and(FULL_CUBE));

    static final PaletteSpec.Context CONTEXT = context(null);

    static PaletteSpec.Context context(String material) {
        return new PaletteSpec.Context() {
            public PaletteGenerator generator() {
                return GENERATOR;
            }

            public Optional<List<String>> preset(String id) {
                return id.equals("greys") ? Optional.of(List.of("white_concrete", "black_concrete")) : Optional.empty();
            }

            public String buildMaterial() {
                return material;
            }
        };
    }

    /** A simple mutable world: a map of positions to block ids. */
    static final class World implements BlockSource {
        final Map<Long, String> blocks = new HashMap<>();

        void set(int x, int y, int z, String id) {
            blocks.put(LongHashSet.pack(x, y, z), id);
        }

        String get(int x, int y, int z) {
            return blocks.get(LongHashSet.pack(x, y, z));
        }

        @Override
        public String blockAt(int x, int y, int z) {
            return get(x, y, z);
        }

        @Override
        public int minY() {
            return -64;
        }

        @Override
        public int maxY() {
            return 319;
        }

        long count(String id) {
            return blocks.values().stream().filter(id::equals).count();
        }
    }

    /**
     * A hollow 5x12x5 stone brick tower at (0,64,0)..(4,75,4) with a stair
     * crown, glass windows, an oak door, standing on a grass/dirt field.
     */
    static World tower() {
        World w = new World();
        for (int x = -10; x <= 14; x++) {
            for (int z = -10; z <= 14; z++) {
                w.set(x, 63, z, "grass_block");
                w.set(x, 62, z, "dirt");
                w.set(x, 61, z, "stone");
            }
        }
        for (int y = 64; y <= 75; y++) {
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                    if (edge) {
                        w.set(x, y, z, y == 75 ? "stone_brick_stairs" : "stone_bricks");
                    }
                }
            }
        }
        w.set(2, 64, 0, "oak_door");
        w.set(2, 65, 0, "oak_door");
        w.set(0, 70, 2, "glass");
        w.set(4, 70, 2, "glass");
        w.set(2, 72, 4, "mossy_stone_bricks");
        return w;
    }
}
