package nl.gzmn.gZMNBuildtools.shared.palette;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class PaletteGeneratorTest {

    /** Stand-in for the runtime check: same name exclusions, shape guessed from the id. */
    static final Predicate<String> FULL_BLOCKS = BlockFilters.anyGlob(BlockFilters.DEFAULT_PALETTE_EXCLUDES).negate()
            .and(id -> !BlockFilters.looksPartial(id));

    static PaletteGenerator generator;

    @BeforeAll
    static void load() {
        generator = new PaletteGenerator(BlockColors.bundled(), FULL_BLOCKS);
    }

    private static double lightness(String id) {
        return generator.colors().side(id).orElseThrow().l();
    }

    @Test
    void okLabRoundTripsRgb() {
        for (int rgb : new int[]{0x000000, 0xFFFFFF, 0x7C7C7C, 0xA12823, 0x29509F}) {
            assertEquals(rgb, OkLab.fromRgb(rgb).toRgb());
        }
        assertTrue(OkLab.fromRgb(0xFFFFFF).l() > OkLab.fromRgb(0x000000).l());
    }

    @Test
    void bundledTableCoversTheGame() {
        BlockColors colors = BlockColors.bundled();
        assertTrue(colors.size() > 1000);
        assertTrue(colors.get("minecraft:stone_bricks").isPresent());
        assertTrue(colors.get("iron_chain").isPresent());
    }

    @Test
    void shadesKeepHueIncludeBaseAndRunDarkToLight() {
        List<String> shades = generator.shades("stone_bricks", 5);
        assertEquals(5, shades.size());
        assertTrue(shades.contains("stone_bricks"));
        for (int i = 1; i < shades.size(); i++) {
            assertTrue(lightness(shades.get(i)) >= lightness(shades.get(i - 1)));
        }
        OkLab base = generator.colors().side("stone_bricks").orElseThrow();
        for (String id : shades) {
            assertTrue(generator.colors().side(id).orElseThrow().chromaDistance(base) < 0.06, id);
        }
    }

    @Test
    void betweenRampsMonotonicallyBetweenEndpoints() {
        List<String> ramp = generator.between("white_concrete", "black_concrete", 6);
        assertEquals("white_concrete", ramp.get(0));
        assertEquals("black_concrete", ramp.get(ramp.size() - 1));
        assertTrue(ramp.size() >= 4);
        for (int i = 1; i < ramp.size(); i++) {
            assertTrue(lightness(ramp.get(i)) < lightness(ramp.get(i - 1)), ramp.toString());
        }
    }

    @Test
    void fillKeepsKeysAndReachesRequestedSize() {
        List<String> filled = generator.fill(List.of("sandstone", "terracotta", "deepslate"), 7);
        assertEquals("sandstone", filled.get(0));
        assertTrue(filled.contains("terracotta"));
        assertEquals("deepslate", filled.get(filled.size() - 1));
        assertTrue(filled.size() >= 5 && filled.size() <= 7, filled.toString());
        assertEquals(filled.size(), filled.stream().distinct().count());
    }

    @Test
    void agedStartsWithMossyAndCrackedVariants() {
        List<String> aged = generator.aged("stone_bricks", 5);
        assertEquals(5, aged.size(), aged.toString());
        assertEquals("mossy_stone_bricks", aged.get(0));
        assertEquals("cracked_stone_bricks", aged.get(1));
        assertTrue(aged.contains("stone_bricks"));
        // Without worn variants it falls back to shades.
        assertEquals(generator.shades("white_concrete", 4), generator.aged("white_concrete", 4));
    }

    @Test
    void rampsStayWithinTheMaterialClass() {
        assertTrue(generator.shades("oak_planks", 5).stream()
                .allMatch(id -> MaterialClass.of(id) == MaterialClass.WOOD), generator.shades("oak_planks", 5).toString());
        assertTrue(generator.shades("stone_bricks", 5).stream()
                .allMatch(id -> MaterialClass.of(id) == MaterialClass.STONE));
    }

    @Test
    void nearestFindsAnExactMatch() {
        OkLab stone = generator.colors().side("red_wool").orElseThrow();
        assertEquals("red_wool", generator.nearest(stone, java.util.Set.of()).orElseThrow());
    }
}
