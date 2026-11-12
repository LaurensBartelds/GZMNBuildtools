package nl.gzmn.gZMNBuildtools.modules.shadow.engine;

import nl.gzmn.gZMNBuildtools.modules.shadow.engine.ShadowPlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.shadow.engine.ShadowPlanner.Plan;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowPlannerTest {

    private static final Bounds ARENA = new Bounds(-50, -64, -50, 50, 320, 50);

    private static Set<Cell> platform(int y, int x0, int x1, int z0, int z1) {
        Set<Cell> cells = new HashSet<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                cells.add(new Cell(x, y, z));
            }
        }
        return cells;
    }

    @Test
    void downCastsTopOfEachColumnOntoTheGround() {
        Set<Cell> build = platform(10, 0, 2, 0, 2);
        ShadowPlanner.SolidOracle ground = (x, y, z) -> y <= 4;
        Plan plan = ShadowPlanner.plan(ARENA, build, ground, 0, -1, 0, 32, -64, 320);
        assertNull(plan.problem());
        assertEquals(9, plan.blockCount());
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                Cell target = new Cell(x, 4, z);
                assertTrue(plan.shadows().containsKey(target), "missing shadow at " + target);
                assertEquals(new Cell(x, 10, z), plan.shadows().get(target));
            }
        }
    }

    @Test
    void projectionNeverLandsOnBuildBlocks() {
        Set<Cell> build = new HashSet<>(platform(10, 0, 2, 0, 2));
        build.addAll(platform(6, 0, 4, 0, 4));
        ShadowPlanner.SolidOracle ground = (x, y, z) -> y <= 4;
        Plan plan = ShadowPlanner.plan(ARENA, build, ground, 0, -1, 0, 32, -64, 320);
        assertNull(plan.problem());
        // 5x5 lower platform casts onto the ground, the 3x3 upper platform's
        // shadow falls through the lower one and also lands on the ground.
        assertEquals(25, plan.blockCount());
        for (Map.Entry<Cell, Cell> e : plan.shadows().entrySet()) {
            assertTrue(e.getKey().y() == 4, "shadow must land on the ground, not in the build");
            assertTrue(!build.contains(e.getKey()));
        }
    }

    @Test
    void horizontalCastsTheWestFaceOntoAWall() {
        Set<Cell> build = new HashSet<>();
        for (int y = 5; y <= 8; y++) {
            for (int z = 0; z <= 2; z++) {
                build.add(new Cell(5, y, z));
            }
        }
        ShadowPlanner.SolidOracle wallAt10 = (x, y, z) -> x == 10 && y >= 5 && y <= 8 && z >= 0 && z <= 2;
        Plan plan = ShadowPlanner.plan(ARENA, build, wallAt10, 1, 0, 0, 32, -64, 320);
        assertNull(plan.problem());
        assertEquals(12, plan.blockCount());
        for (int y = 5; y <= 8; y++) {
            for (int z = 0; z <= 2; z++) {
                assertTrue(plan.shadows().containsKey(new Cell(10, y, z)));
            }
        }
    }

    @Test
    void distLimitsHowFarShadowsTravel() {
        Set<Cell> build = platform(10, 0, 0, 0, 0);
        ShadowPlanner.SolidOracle farGround = (x, y, z) -> y <= -20;
        Plan plan = ShadowPlanner.plan(ARENA, build, farGround, 0, -1, 0, 8, -64, 320);
        assertNull(plan.problem());
        assertEquals(0, plan.blockCount());
    }

    @Test
    void emptyBuildReportsProblem() {
        Plan plan = ShadowPlanner.plan(ARENA, Set.of(), (x, y, z) -> true, 0, -1, 0, 32, -64, 320);
        assertEquals(0, plan.blockCount());
        assertTrue(plan.problem() != null);
    }

    @Test
    void zeroDirectionReportsProblem() {
        Plan plan = ShadowPlanner.plan(ARENA, platform(10, 0, 0, 0, 0), (x, y, z) -> true, 0, 0, 0, 32, -64, 320);
        assertTrue(plan.problem() != null);
    }
}
