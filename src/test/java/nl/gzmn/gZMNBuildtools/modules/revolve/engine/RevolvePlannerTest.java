package nl.gzmn.gZMNBuildtools.modules.revolve.engine;

import nl.gzmn.gZMNBuildtools.modules.revolve.engine.RevolvePlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.revolve.engine.RevolvePlanner.Plan;
import nl.gzmn.gZMNBuildtools.modules.revolve.engine.RevolvePlanner.Placement;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RevolvePlannerTest {

    @Test
    void quarterTurnMappingAroundY() {
        Plan plan = RevolvePlanner.plan(new Bounds(0, 64, 0, 2, 64, 2), new int[]{1, 64, 1}, 'y', 360, 4);
        assertNull(plan.problem());
        assertEquals(33, plan.blockCount());
        Placement p = plan.placements().get(new Cell(1, 64, 0));
        assertNotNull(p);
        assertEquals(new Cell(2, 64, 1), p.source());
        assertEquals(1, p.quarterTurns());
    }

    @Test
    void partialRevolve() {
        Plan plan = RevolvePlanner.plan(new Bounds(0, 64, 0, 2, 64, 2), new int[]{1, 64, 1}, 'y', 180, 2);
        assertNull(plan.problem());
        assertEquals(17, plan.blockCount());
        Placement turned = plan.placements().get(new Cell(1, 64, 0));
        assertEquals(new Cell(2, 64, 1), turned.source());
        assertEquals(1, turned.quarterTurns());
        Placement straight = plan.placements().get(new Cell(2, 64, 1));
        assertEquals(new Cell(2, 64, 1), straight.source());
        assertEquals(0, straight.quarterTurns());
    }

    @Test
    void revolveAroundX() {
        Plan plan = RevolvePlanner.plan(new Bounds(0, 64, 0, 0, 66, 0), new int[]{0, 65, 0}, 'x', 360, 4);
        assertNull(plan.problem());
        assertEquals(9, plan.blockCount());
        Placement p = plan.placements().get(new Cell(0, 65, 1));
        assertNotNull(p);
        assertEquals(new Cell(0, 66, 0), p.source());
        assertEquals(1, p.quarterTurns());
    }

    @Test
    void emptySelection() {
        Plan plan = RevolvePlanner.plan(new Bounds(0, 64, 0, -1, 64, -1), new int[]{0, 64, 0}, 'y', 360, 4);
        assertNotNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }
}
