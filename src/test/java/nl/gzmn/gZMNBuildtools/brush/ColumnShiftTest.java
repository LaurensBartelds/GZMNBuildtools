package nl.gzmn.gZMNBuildtools.brush;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnShiftTest {

    @Test
    void raisingMovesSurfaceLayersUpAndFillsGapWithDeepestLayer() {
        ColumnShift shift = ColumnShift.plan(64, 3, -64, 319);
        // Surface block (64) now sits at 67, decorations above it move too.
        assertEquals(64, shift.sourceFor(67));
        assertEquals(65, shift.sourceFor(68));
        assertEquals(66, shift.sourceFor(69));
        // Layers move up: 63 -> 66, 60 (bottom of layer stack) -> 63.
        assertEquals(63, shift.sourceFor(66));
        assertEquals(60, shift.sourceFor(63));
        // Gap below the moved stack is filled with the deepest layer.
        assertEquals(60, shift.sourceFor(60));
        assertEquals(60, shift.sourceFor(62));
        assertEquals(60, shift.fromY());
        assertEquals(69, shift.toY());
    }

    @Test
    void loweringMovesLayersDownAndClearsAbove() {
        ColumnShift shift = ColumnShift.plan(64, -2, -64, 319);
        assertEquals(64, shift.sourceFor(62));
        assertEquals(60, shift.sourceFor(58));
        assertEquals(66, shift.sourceFor(64));
        assertEquals(ColumnShift.AIR, shift.sourceFor(65));
        assertEquals(ColumnShift.AIR, shift.sourceFor(66));
        assertEquals(58, shift.fromY());
        assertEquals(66, shift.toY());
    }

    @Test
    void shiftIsClampedToWorldHeight() {
        ColumnShift shift = ColumnShift.plan(318, 5, -64, 319);
        assertEquals(319, shift.toY());
    }
}
