package nl.gzmn.gZMNBuildtools.brush;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrushArgumentsTest {

    @Test
    void defaultsApplyWhenNothingGiven() {
        BrushArguments<TerrainBrushType> args = BrushArguments.parse(TerrainBrushType.RAISE, "");
        assertEquals(3, args.get("height"));
        assertNull(args.pattern());
    }

    @Test
    void positionalAndNamedArgumentsMix() {
        BrushArguments<TerrainBrushType> args = BrushArguments.parse(TerrainBrushType.SMOOTH, "3 iterations=5");
        assertEquals(3, args.getInt("kernel"));
        assertEquals(5, args.getInt("iterations"));
        assertEquals(1, args.get("strength"));
    }

    @Test
    void patternBrushesTakePatternFirst() {
        BrushArguments<TerrainBrushType> args = BrushArguments.parse(TerrainBrushType.BLOB, "50%stone,50%andesite roughness=0.7");
        assertEquals("50%stone,50%andesite", args.pattern());
        assertEquals(0.7, args.get("roughness"), 1e-9);
    }

    @Test
    void rejectsMissingPatternUnknownNamesAndOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.OVERLAY, ""));
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.RAISE, "bogus=1"));
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.RAISE, "999"));
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.RAISE, "abc"));
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.RAISE, "1 0 5"));
    }
}
