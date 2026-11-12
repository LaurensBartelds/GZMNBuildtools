package nl.gzmn.gZMNBuildtools.modules.roof.engine;

import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.ColumnTops;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Part;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner.Plan;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static RoofOptions opts(String style, int height, int overhang, double noise,
                                    boolean stackDown, boolean levelEnds, long seed) {
        return new RoofOptions(style, "auto", height, overhang, noise, stackDown, levelEnds, seed);
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

    @Test
    void noBuild() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(-1), opts("gable"));
        assertNotNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }

    @Test
    void hipSevenByFive() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 6, 64, 4), flat(64), opts("hip"));
        assertNull(plan.problem());
        assertEquals(35, plan.blockCount());
        Part center = plan.placements().get(new Cell(3, 67, 2));
        assertEquals(ShapeKind.SLAB, center.kind());
        Part corner = plan.placements().get(new Cell(0, 65, 0));
        assertEquals(ShapeKind.STAIRS, corner.kind());
        assertEquals("north", corner.facing());
        assertTrue(yAt(plan, 0, 0) < yAt(plan, 3, 2));
        Part end = plan.placements().get(new Cell(0, 66, 2));
        assertEquals(ShapeKind.STAIRS, end.kind());
        assertEquals("west", end.facing());
    }

    private static int yAt(Plan plan, int x, int z) {
        return plan.placements().keySet().stream()
                .filter(c -> c.x() == x && c.z() == z).findFirst().orElseThrow().y();
    }

    @Test
    void pyramidFiveByFive() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(64), opts("pyramid"));
        assertNull(plan.problem());
        assertEquals(25, plan.blockCount());
        assertEquals(ShapeKind.SLAB, plan.placements().get(new Cell(2, 67, 2)).kind());
        Part corner = plan.placements().get(new Cell(0, 65, 0));
        assertEquals(ShapeKind.STAIRS, corner.kind());
        assertEquals("north", corner.facing());
    }

    @Test
    void overhangExtendsPastWalls() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(64), opts("gable", 2, 1, 0, false, false, 42));
        assertNull(plan.problem());
        assertEquals(49, plan.blockCount());
        Part overhang = plan.placements().get(new Cell(2, 65, -1));
        assertNotNull(overhang);
        assertEquals(ShapeKind.STAIRS, overhang.kind());
        assertEquals("north", overhang.facing());
    }

    @Test
    void noiseIsDeterministicForSameSeed() {
        Bounds bounds = new Bounds(0, 64, 0, 8, 64, 8);
        RoofOptions noisy = opts("gable", 6, 0, 4, false, false, 42);
        Plan first = RoofPlanner.plan(bounds, flat(64), noisy);
        Plan second = RoofPlanner.plan(bounds, flat(64), noisy);
        assertEquals(first.placements(), second.placements());
        Plan calm = RoofPlanner.plan(bounds, flat(64), opts("gable", 6, 0, 0, false, false, 42));
        assertEquals(calm.blockCount(), first.blockCount());
    }

    @Test
    void stackDownFillsBelowTheRoof() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), flat(64), opts("gable", 2, 0, 0, true, false, 42));
        assertNull(plan.problem());
        assertEquals(45, plan.blockCount());
        assertEquals(ShapeKind.FULL, plan.placements().get(new Cell(0, 65, 2)).kind());
        assertEquals(ShapeKind.FULL, plan.placements().get(new Cell(0, 66, 2)).kind());
    }

    @Test
    void levelEndsGiveFullHeightAtTheEnds() {
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 6, 64, 4), flat(64), opts("gable", 2, 0, 0, false, true, 42));
        assertNull(plan.problem());
        assertEquals(35, plan.blockCount());
        assertEquals(ShapeKind.SLAB, plan.placements().get(new Cell(0, 67, 2)).kind());
        assertEquals(ShapeKind.SLAB, plan.placements().get(new Cell(6, 67, 0)).kind());
    }

    @Test
    void holeInTheBuildGetsNoRoof() {
        ColumnTops lShape = (x, z) -> (x == 0 && z == 0) ? -1 : 64;
        Plan plan = RoofPlanner.plan(new Bounds(0, 64, 0, 4, 64, 4), lShape, opts("gable"));
        assertNull(plan.problem());
        assertEquals(24, plan.blockCount());
        assertFalse(plan.placements().containsKey(new Cell(0, 65, 0)));
    }
}
