package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainTextureTest {

    private static final Predicate<String> IS_BLOCK = BuildFixtures.COLORS.ids()::contains;
    private static final Set<String> NOT_GROUND = Set.of("short_grass", "tall_grass", "poppy", "water", "oak_leaves",
            "oak_log");
    private static final Texturer.Plants PLANTS = new Texturer.Plants(Set.of("short_grass", "poppy")::contains,
            Set.of("grass_block", "dirt", "podzol")::contains);

    private static final String TEST_PRESETS = """
            presets:
              test:
                description: rules with exact outcomes
                rules:
                  - when: { slope: "45..", depth: "0..2" }
                    place: [ stone ]
                  - when: { height: "70.." }
                    place: [ snow_block ]
                  - when: { }
                    place: [ sand ]
                  - when: { depth: "1..2" }
                    place: [ gravel ]
              stripes:
                description: bands
                rules:
                  - when: { depth: "0..9", slope: "0..90" }
                    bands: [ terracotta, orange_terracotta, white_terracotta ]
                    band-height: 2
            """;

    /**
     * Flat grass at y=60 over dirt for x 0..29, then a steep cliff rising 3 blocks
     * per step for x 30..39 up to a plateau at y=90 for x 40..47; z 0..9. A plank
     * hut stands on the flat at x 5..7.
     */
    private static BuildFixtures.World landscape() {
        BuildFixtures.World w = new BuildFixtures.World();
        for (int x = 0; x <= 47; x++) {
            int top = x < 30 ? 60 : 60 + (Math.min(x, 39) - 29) * 3;
            for (int z = 0; z <= 9; z++) {
                for (int y = 45; y < top; y++) {
                    w.set(x, y, z, "dirt");
                }
                w.set(x, top, z, "grass_block");
            }
        }
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                for (int y = 61; y <= 63; y++) {
                    w.set(x, y, z, "oak_planks");
                }
            }
        }
        return w;
    }

    private static TexturePresets parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return TexturePresets.parse(config, IS_BLOCK);
    }

    private static int run(BuildFixtures.World world, TexturePreset preset, Bounds region, Texturer.Inside inside) {
        Predicate<String> ground = id -> !NOT_GROUND.contains(id);
        Predicate<String> paintable = id -> BuildFixtures.RULES.isTerrain(id) || preset.blocks().contains(id);
        return new Texturer(preset, ground, paintable, PLANTS, 42).apply(world, region, inside, world::set);
    }

    private static final Bounds ALL = new Bounds(0, 40, 0, 47, 100, 9);

    @Test
    void picksBlocksByDepthSlopeAndHeight() throws Exception {
        BuildFixtures.World w = landscape();
        run(w, parse(TEST_PRESETS).get("test").orElseThrow(), ALL, (x, y, z) -> true);

        assertEquals("sand", w.get(15, 60, 5), "flat surface");
        assertEquals("gravel", w.get(15, 59, 5), "one below the surface");
        assertEquals("gravel", w.get(15, 58, 5), "two below the surface");
        assertEquals("dirt", w.get(15, 57, 5), "deeper than any rule is left alone");
        assertEquals("stone", w.get(34, 75, 5), "cliff face");
        assertEquals("stone", w.get(34, 73, 5), "cliff face, two deep");
        assertEquals("snow_block", w.get(44, 90, 2), "plateau above y=70");
        assertEquals("gravel", w.get(44, 89, 2), "under the plateau");
    }

    @Test
    void leavesBuildsAloneAndClearsPlantsOffNewGround() throws Exception {
        BuildFixtures.World w = landscape();
        w.set(20, 61, 3, "short_grass");
        w.set(21, 61, 3, "poppy");
        w.set(12, 66, 3, "oak_leaves");
        run(w, parse(TEST_PRESETS).get("test").orElseThrow(), ALL, (x, y, z) -> true);

        for (int y = 61; y <= 63; y++) {
            assertEquals("oak_planks", w.get(6, y, 6), "the hut is not terrain");
        }
        assertNull(w.get(20, 61, 3), "grass tuft removed from the sand");
        assertNull(w.get(21, 61, 3), "flower removed from the sand");
        assertEquals("sand", w.get(12, 60, 3), "leaves above do not hide the ground");
        assertEquals("oak_leaves", w.get(12, 66, 3));
    }

    @Test
    void staysInsideTheRegion() throws Exception {
        BuildFixtures.World w = landscape();
        run(w, parse(TEST_PRESETS).get("test").orElseThrow(), ALL, (x, y, z) -> x < 10);
        assertEquals("sand", w.get(9, 60, 1));
        assertEquals("grass_block", w.get(10, 60, 1));
    }

    @Test
    void piecesLineUpWithTheWhole() throws Exception {
        TexturePreset forest = bundled().get("forest").orElseThrow();
        BuildFixtures.World whole = landscape();
        BuildFixtures.World halves = landscape();
        run(whole, forest, ALL, (x, y, z) -> true);
        run(halves, forest, new Bounds(0, 40, 0, 23, 100, 9), (x, y, z) -> true);
        run(halves, forest, new Bounds(24, 40, 0, 47, 100, 9), (x, y, z) -> true);
        for (int x = 0; x <= 47; x++) {
            for (int y = 45; y <= 100; y++) {
                for (int z = 0; z <= 9; z++) {
                    assertEquals(whole.get(x, y, z), halves.get(x, y, z), "at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void bandsFollowHeight() throws Exception {
        BuildFixtures.World w = landscape();
        run(w, parse(TEST_PRESETS).get("stripes").orElseThrow(), ALL, (x, y, z) -> true);
        Map<Integer, String> byY = new HashMap<>();
        for (int x = 30; x <= 39; x++) {
            for (int z = 0; z <= 9; z++) {
                for (int y = 60; y <= 90; y++) {
                    String id = w.get(x, y, z);
                    if (id != null && id.endsWith("terracotta")) {
                        String before = byY.putIfAbsent(y, id);
                        assertTrue(before == null || before.equals(id), "one band per height at y=" + y);
                    }
                }
            }
        }
        assertEquals("terracotta", byY.get(60));
        assertEquals("terracotta", byY.get(61));
        assertEquals("orange_terracotta", byY.get(62));
        assertEquals("white_terracotta", byY.get(64));
        assertEquals("terracotta", byY.get(66));
    }

    @Test
    void weightsSplitRoughlyAsWritten() throws Exception {
        TexturePreset preset = parse("""
                presets:
                  mix:
                    rules:
                      - place: [ "75%stone", "25%andesite" ]
                """).get("mix").orElseThrow();
        BuildFixtures.World w = landscape();
        run(w, preset, new Bounds(0, 40, 0, 29, 100, 9), (x, y, z) -> true);
        long stone = w.count("stone");
        long andesite = w.count("andesite");
        assertEquals(300 - 9, stone + andesite, "every open surface block outside the hut");
        assertTrue(stone > 180 && stone < 260, "about 75% stone, got " + stone);
    }

    @Test
    void reportsBrokenPresetsAndKeepsTheRest() throws Exception {
        TexturePresets presets = parse("""
                presets:
                  good:
                    rules:
                      - place: [ sand ]
                  typo:
                    rules:
                      - place: [ sandd ]
                  empty:
                    description: no rules
                  badrange:
                    rules:
                      - when: { slope: "steep" }
                        place: [ stone ]
                  badweight:
                    rules:
                      - place: [ "lots%stone" ]
                """);
        assertEquals(Set.of("good"), presets.ids());
        assertEquals(4, presets.problems().size(), presets.problems().toString());
        assertTrue(presets.problems().stream().anyMatch(p -> p.contains("unknown block 'sandd'")));
        assertTrue(presets.problems().stream().anyMatch(p -> p.contains("'empty' has no rules")));
        assertTrue(presets.problems().stream().anyMatch(p -> p.contains("Not a range: 'steep'")));
    }

    @Test
    void bundledPresetsAllLoad() throws Exception {
        TexturePresets presets = bundled();
        assertEquals(Set.of("alpine", "desert", "forest", "badlands", "tundra", "beach"), presets.ids());
        assertEquals(java.util.List.of(), presets.problems());
        for (TexturePreset preset : presets.byId().values()) {
            BuildFixtures.World w = landscape();
            assertTrue(run(w, preset, ALL, (x, y, z) -> true) > 300, preset.id() + " changes the landscape");
            for (int y = 61; y <= 63; y++) {
                assertEquals("oak_planks", w.get(6, y, 6), preset.id() + " leaves the hut alone");
            }
        }
    }

    @Test
    void parsesRanges() {
        assertEquals(new Range(10, 20), Range.parse("10..20"));
        assertEquals(new Range(10, Double.POSITIVE_INFINITY), Range.parse("10.."));
        assertEquals(new Range(Double.NEGATIVE_INFINITY, 0.5), Range.parse("..0.5"));
        assertEquals(new Range(3, 3), Range.parse(" 3 "));
        assertThrows(IllegalArgumentException.class, () -> Range.parse("ten"));
    }

    private static TexturePresets bundled() throws Exception {
        try (var in = TerrainTextureTest.class.getResourceAsStream("/terrain-presets.yml")) {
            return TexturePresets.parse(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)), IS_BLOCK);
        }
    }
}
