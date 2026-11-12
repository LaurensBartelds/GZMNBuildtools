package nl.gzmn.gZMNBuildtools.modules.resize.engine;

import nl.gzmn.gZMNBuildtools.modules.resize.engine.ResizePlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.resize.engine.ResizePlanner.Plan;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResizePlannerTest {

    @Test
    void doubleScale() {
        Plan plan = ResizePlanner.plan(new Bounds(0, 64, 0, 3, 65, 3), 2, 2, 2);
        assertNull(plan.problem());
        assertEquals(256, plan.blockCount());
        assertEquals(new Bounds(0, 64, 0, 7, 67, 7), plan.out());
        assertEquals(new Cell(1, 64, 0), plan.placements().get(new Cell(2, 64, 0)));
        assertEquals(new Cell(3, 65, 3), plan.placements().get(new Cell(7, 67, 7)));
    }

    @Test
    void halfScale() {
        Plan plan = ResizePlanner.plan(new Bounds(0, 64, 0, 3, 65, 3), 0.5, 0.5, 0.5);
        assertNull(plan.problem());
        assertEquals(4, plan.blockCount());
        assertEquals(new Cell(2, 64, 2), plan.placements().get(new Cell(1, 64, 1)));
    }

    @Test
    void nonUniformScale() {
        Plan plan = ResizePlanner.plan(new Bounds(0, 64, 0, 3, 65, 3), 2, 1, 0.5);
        assertNull(plan.problem());
        assertEquals(32, plan.blockCount());
        assertEquals(new Bounds(0, 64, 0, 7, 65, 1), plan.out());
    }

    @Test
    void emptySelection() {
        Plan plan = ResizePlanner.plan(new Bounds(0, 64, 0, -1, 64, -1), 2, 2, 2);
        assertNotNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }
}
