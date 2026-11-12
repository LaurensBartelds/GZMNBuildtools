package nl.gzmn.gZMNBuildtools.modules.loft.engine;

import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftPlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftPlanner.ColumnGround;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftPlanner.Plan;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftPlanner.Station;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoftPlannerTest {

    private static final ColumnGround NONE = (x, z) -> -1;

    private static LoftOptions opts(boolean fill, boolean smooth, boolean stackDown) {
        return new LoftOptions(true, fill, smooth, false, stackDown, 3, 42);
    }

    private static List<Station> straight() {
        return List.of(new Station(0, 64, 0, 4, 4), new Station(10, 64, 0, 4, 4));
    }

    @Test
    void twoPointsStraightLine() {
        Plan plan = LoftPlanner.plan(straight(), opts(false, true, false), NONE);
        assertNull(plan.problem());
        assertTrue(plan.blockCount() > 50);
        for (Cell c : plan.cells()) {
            assertTrue(c.x() >= 0 && c.x() <= 10, "x out of range: " + c);
            assertTrue(c.y() >= 62 && c.y() <= 66, "y out of range: " + c);
            assertTrue(Math.abs(c.z()) <= 2, "z out of range: " + c);
        }
        Plan again = LoftPlanner.plan(straight(), opts(false, true, false), NONE);
        assertEquals(plan.cells(), again.cells());
    }

    @Test
    void fillContainsCentreOutlineDoesNot() {
        Plan outline = LoftPlanner.plan(straight(), opts(false, true, false), NONE);
        Plan fill = LoftPlanner.plan(straight(), opts(true, true, false), NONE);
        assertFalse(outline.cells().contains(new Cell(5, 64, 0)));
        assertTrue(fill.cells().contains(new Cell(5, 64, 0)));
        assertTrue(fill.blockCount() >= outline.blockCount());
    }

    @Test
    void framesBlendAlongTheCurve() {
        List<Station> taper = List.of(new Station(0, 64, 0, 2, 2), new Station(10, 64, 0, 6, 6));
        Plan plan = LoftPlanner.plan(taper, opts(false, true, false), NONE);
        assertNull(plan.problem());
        assertEquals(1, maxAbsZ(plan, 0, 1));
        assertEquals(3, maxAbsZ(plan, 9, 10));
    }

    private static int maxAbsZ(Plan plan, int xMin, int xMax) {
        int max = 0;
        for (Cell c : plan.cells()) {
            if (c.x() >= xMin && c.x() <= xMax) {
                max = Math.max(max, Math.abs(c.z()));
            }
        }
        return max;
    }

    @Test
    void tooFewStations() {
        Plan plan = LoftPlanner.plan(List.of(new Station(0, 64, 0, 4, 4)), opts(false, true, false), NONE);
        assertNotNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }

    @Test
    void stackDownFillsToTheGround() {
        ColumnGround ground = (x, z) -> 60;
        Plan outline = LoftPlanner.plan(straight(), opts(false, true, false), NONE);
        Plan stacked = LoftPlanner.plan(straight(), opts(false, true, true), ground);
        assertNull(stacked.problem());
        assertTrue(stacked.cells().contains(new Cell(5, 61, 0)));
        assertTrue(stacked.blockCount() > outline.blockCount());
    }
}
