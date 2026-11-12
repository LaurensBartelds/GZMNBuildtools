package nl.gzmn.gZMNBuildtools.modules.fixconnect.engine;

import nl.gzmn.gZMNBuildtools.modules.fixconnect.engine.FixConnectPlanner.Cell;
import nl.gzmn.gZMNBuildtools.modules.fixconnect.engine.FixConnectPlanner.Plan;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixConnectPlannerTest {

    private static final Predicate<String> FULL_CUBE = Set.of("stone", "oak_planks", "bricks");

    private static final class World {
        final Map<String, String> blocks = new HashMap<>();
        final Map<String, boolean[]> connections = new HashMap<>();

        void put(int x, int y, int z, String id) {
            blocks.put(x + "," + y + "," + z, id);
        }

        void connect(int x, int y, int z, boolean n, boolean e, boolean s, boolean w) {
            connections.put(x + "," + y + "," + z, new boolean[]{n, e, s, w});
        }

        FixConnectPlanner.BlockOracle oracle() {
            return (x, y, z) -> blocks.get(x + "," + y + "," + z);
        }

        FixConnectPlanner.ConnectionOracle current() {
            return (x, y, z) -> connections.getOrDefault(x + "," + y + "," + z, new boolean[4]);
        }
    }

    @Test
    void fenceNextToSolidBlockConnects() {
        World world = new World();
        world.put(0, 64, 0, "oak_fence");
        world.put(0, 64, -1, "stone");
        Plan plan = FixConnectPlanner.plan(new Bounds(0, 64, 0, 0, 64, 0), world.oracle(), FULL_CUBE, world.current());
        assertNull(plan.problem());
        assertEquals(1, plan.blockCount());
        assertArrayEquals(new boolean[]{true, false, false, false}, plan.changes().get(new Cell(0, 64, 0)));
    }

    @Test
    void sameFamilyConnectsAndOtherFamilyDoesNot() {
        World world = new World();
        world.put(0, 64, 0, "oak_fence");
        world.put(1, 64, 0, "birch_fence");       // east: same family -> connect
        world.put(0, 64, 1, "stone_wall");        // south: different family, not full cube -> no
        world.put(-1, 64, 0, "stone");            // west: full cube -> connect
        Plan plan = FixConnectPlanner.plan(new Bounds(0, 64, 0, 0, 64, 0), world.oracle(), FULL_CUBE, world.current());
        assertEquals(1, plan.blockCount());
        assertArrayEquals(new boolean[]{false, true, false, true}, plan.changes().get(new Cell(0, 64, 0)));
    }

    @Test
    void alreadyCorrectConnectionsYieldNoChanges() {
        World world = new World();
        world.put(0, 64, 0, "glass_pane");
        world.put(0, 64, -1, "glass_pane");
        world.connect(0, 64, 0, true, false, false, false);
        Plan plan = FixConnectPlanner.plan(new Bounds(0, 64, 0, 0, 64, 0), world.oracle(), FULL_CUBE, world.current());
        assertEquals(0, plan.blockCount());
        assertTrue(plan.changes().isEmpty());
    }

    @Test
    void staleConnectionIsDropped() {
        World world = new World();
        world.put(0, 64, 0, "iron_bars");
        world.connect(0, 64, 0, true, true, true, true); // nothing around
        Plan plan = FixConnectPlanner.plan(new Bounds(0, 64, 0, 0, 64, 0), world.oracle(), FULL_CUBE, world.current());
        assertEquals(1, plan.blockCount());
        assertArrayEquals(new boolean[]{false, false, false, false}, plan.changes().get(new Cell(0, 64, 0)));
    }

    @Test
    void fenceGatesAndPlainBlocksAreSkipped() {
        World world = new World();
        world.put(0, 64, 0, "oak_fence_gate");
        world.put(1, 64, 0, "stone");
        world.put(2, 64, 0, "oak_planks");
        Plan plan = FixConnectPlanner.plan(new Bounds(0, 64, 0, 2, 64, 0), world.oracle(), FULL_CUBE, world.current());
        assertEquals(0, plan.blockCount());
    }

    @Test
    void neighboursOutsideTheSelectionStillCount() {
        World world = new World();
        world.put(5, 64, 5, "brick_wall");   // outside the selection below
        world.put(5, 64, 4, "brick_wall");   // at the selection edge
        Plan plan = FixConnectPlanner.plan(new Bounds(5, 64, 4, 5, 64, 4), world.oracle(), FULL_CUBE, world.current());
        assertEquals(1, plan.blockCount());
        assertArrayEquals(new boolean[]{true, false, false, false}, plan.changes().get(new Cell(5, 64, 4)));
    }

    @Test
    void familyOfClassifiesIds() {
        assertEquals("fence", FixConnectPlanner.familyOf("oak_fence"));
        assertNull(FixConnectPlanner.familyOf("oak_fence_gate"));
        assertEquals("wall", FixConnectPlanner.familyOf("mud_brick_wall"));
        assertEquals("pane", FixConnectPlanner.familyOf("glass_pane"));
        assertEquals("pane", FixConnectPlanner.familyOf("iron_bars"));
        assertNull(FixConnectPlanner.familyOf("stone"));
        assertNull(FixConnectPlanner.familyOf(null));
        assertFalse(FixConnectPlanner.familyOf("stone_stairs") != null);
    }

    private static void assertArrayEquals(boolean[] expected, boolean[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
