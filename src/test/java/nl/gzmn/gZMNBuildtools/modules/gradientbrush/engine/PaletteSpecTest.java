package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.SmartFixtures.*;
import static nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class PaletteSpecTest {

    private static PaletteSpec parse(String s) {
        return PaletteSpec.parse(s, CATALOG::exists);
    }

    @Test
    void parsesEveryForm() {
        assertEquals(PaletteSpec.Kind.AUTO, parse("auto").kind());
        assertEquals(PaletteSpec.Kind.AGED, parse("aged:stone_bricks").kind());
        assertEquals(PaletteSpec.Kind.SHADES, parse("shades:minecraft:oak_planks").kind());
        assertEquals(PaletteSpec.Kind.BETWEEN, parse("sandstone..deepslate").kind());
        assertEquals(PaletteSpec.Kind.PRESET, parse("preset:greys").kind());
        assertEquals(PaletteSpec.Kind.LIST, parse("stone,andesite").kind());
        assertEquals(PaletteSpec.Kind.LIST, parse("[50%stone,50%andesite][deepslate]").kind());
    }

    @Test
    void resolvesExplicitAndGeneratedPalettes() {
        assertEquals(List.of("stone", "andesite"), List.copyOf(parse("stone,andesite").resolve(CONTEXT, 0).blocks()));
        Palette mixed = parse("[50%stone,50%andesite][deepslate]").resolve(CONTEXT, 0);
        assertEquals(2, mixed.stops().size());
        assertEquals(2, mixed.stops().get(0).blocks().size());
        assertEquals(5, parse("aged:stone_bricks").resolve(CONTEXT, 0).stops().size());
        assertEquals(7, parse("shades:stone_bricks").resolve(CONTEXT, 7).stops().size());
        assertEquals("white_concrete", parse("preset:greys").resolve(CONTEXT, 0).stops().get(0).pick(0));
        // #hex picks the closest block.
        assertEquals("red_wool", parse("#A12823").resolve(CONTEXT, 0).stops().get(0).pick(0));
        // steps fills in between explicit keys.
        assertTrue(parse("white_concrete,black_concrete").resolve(CONTEXT, 5).stops().size() >= 4);
    }

    @Test
    void autoUsesTheBuildMaterial() {
        Palette auto = parse("auto").resolve(context("stone_bricks"), 0);
        assertEquals("mossy_stone_bricks", auto.stops().get(0).pick(0));
        assertThrows(IllegalArgumentException.class, () -> parse("auto").resolve(CONTEXT, 0));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> parse("stone,notablock"));
        assertThrows(IllegalArgumentException.class, () -> parse("[stone"));
        assertThrows(IllegalArgumentException.class, () -> parse("#12"));
        assertThrows(IllegalArgumentException.class, () -> parse("abc%stone"));
        assertThrows(IllegalArgumentException.class, () -> parse("aged:nothing"));
    }

    @Test
    void settingsParseOptionsAndRejectNonsense() {
        GradientSettings s = GradientSettings.defaults()
                .with("axis", "down", 32).with("target", "main", 32).with("dither", "noise", 32)
                .with("softness", "0.3", 32).with("shapes", "full", 32).with("range", "120..60", 32)
                .with("radius", "8", 32).with("steps", "6", 32);
        assertEquals(GradientAxis.DOWN, s.axis());
        assertEquals(TargetMode.MAIN, s.target());
        assertEquals(Dither.NOISE, s.dither());
        assertEquals(0.3, s.softness(), 1e-9);
        assertFalse(s.keepShapes());
        assertArrayEquals(new double[]{60, 120}, s.range());
        assertEquals(8, s.radius());
        assertEquals(6, s.steps());
        assertThrows(IllegalArgumentException.class, () -> s.with("axis", "sideways", 32));
        assertThrows(IllegalArgumentException.class, () -> s.with("radius", "99", 32));
        assertThrows(IllegalArgumentException.class, () -> s.with("bogus", "1", 32));
    }
}
