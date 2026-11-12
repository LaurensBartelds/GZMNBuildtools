package nl.gzmn.gZMNBuildtools.modules.roof.engine;

import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.ColumnTops;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Part;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Plan;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoofPlannerTest {

    private static ColumnTops flat(int top) {
        return (x, z) -> top;
    }

    private static RoofOptions opts(String style) {
        return new RoofOptions(style, "auto", 2, 0, 0, false, false, 42);
    }

    @Test
    void gableFiveByFive() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(64), opts("gable"));
        assertNull(plan.problem());
        assertEquals(25, plan.blockCount());
        Part ridge = plan.placements().get(new Cell(0, 67, 2));
        assertNotNull(ridge);
        assertEquals(ShapeKind.SLAB, ridge.kind());
        assertTrue(ridge.topHalf());
        Part northEave = plan.placements().get(new Cell(0, 65, 0));
        assertEquals(ShapeKind.STAIRS, northEave.kind());
        assertEquals("north", northEave.facing());
        Part southEave = plan.placements().get(new Cell(0, 65, 4));
        assertEquals(ShapeKind.STAIRS, southEave.kind());
        assertEquals("south", southEave.facing());
        Part mid = plan.placements().get(new Cell(0, 66, 1));
        assertEquals(ShapeKind.STAIRS, mid.kind());
        assertEquals("north", mid.facing());
    }

    @Test
    void flatFiveByFive() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(64), opts("flat"));
        assertNull(plan.problem());
        assertEquals(25, plan.blockCount());
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                Part part = plan.placements().get(new Cell(x, 65, z));
                assertNotNull(part);
                assertEquals(ShapeKind.SLAB, part.kind());
                assertTrue(part.topHalf());
            }
        }
    }

    @Test
    void tooSmall() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 1, 64, 1), flat(64), opts("gable"));
        assertNotNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }
}
