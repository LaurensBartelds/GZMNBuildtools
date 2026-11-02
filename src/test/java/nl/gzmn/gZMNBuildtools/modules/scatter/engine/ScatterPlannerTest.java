package nl.gzmn.gZMNBuildtools.modules.scatter.engine;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScatterPlannerTest {

    private static final Set<String> NOT_SOLID = Set.of("water", "short_grass");
    private static final ScatterPlanner.Rules RULES = new ScatterPlanner.Rules(
            id -> !NOT_SOLID.contains(id) && !id.endsWith("_leaves") && !id.endsWith("_log"),
            id -> !NOT_SOLID.contains(id),
            BuildFixtures.RULES::isTerrain,
            id -> id.equals("water"));

    private static final Footprint TREE = new Footprint(List.of(new int[]{0, 0}), 3, 8);
    private static final Footprint WIDE = new Footprint(List.of(new int[]{-1, 0}, new int[]{0, 0}, new int[]{1, 0},
            new int[]{2, 1}), 3, 3);

    /** Grass at y=60 over x/z -30..30. */
    private static BuildFixtures.World meadow() {
        BuildFixtures.World w = new BuildFixtures.World();
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                w.set(x, 59, z, "dirt");
                w.set(x, 60, z, "grass_block");
            }
        }
        return w;
    }

    private static List<ScatterPlanner.Placement> plan(BuildFixtures.World w, List<Footprint> prefabs,
                                                       ScatterOptions options, long seed) {
        return ScatterPlanner.plan(w, RULES, prefabs, 0, 60, 0, 14, options, seed);
    }

    @Test
    void spreadsPrefabsApartOnTheGround() {
        List<ScatterPlanner.Placement> plan = plan(meadow(), List.of(TREE), ScatterOptions.DEFAULTS, 1);
        assertTrue(plan.size() >= 5, "a decent scatter, got " + plan.size());
        for (ScatterPlanner.Placement p : plan) {
            assertEquals(61, p.y(), "sits on the grass");
            assertTrue(Math.hypot(p.x(), p.z()) <= 14.5, "inside the brush");
        }
        for (int i = 0; i < plan.size(); i++) {
            for (int j = i + 1; j < plan.size(); j++) {
                ScatterPlanner.Placement a = plan.get(i);
                ScatterPlanner.Placement b = plan.get(j);
                assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= 0.8 * 6 + 1 - 1e-9, "kept apart");
            }
        }
    }

    @Test
    void sameSeedSameLayout() {
        List<ScatterPlanner.Placement> a = plan(meadow(), List.of(TREE, WIDE), ScatterOptions.DEFAULTS, 99);
        List<ScatterPlanner.Placement> b = plan(meadow(), List.of(TREE, WIDE), ScatterOptions.DEFAULTS, 99);
        assertEquals(a, b);
        assertFalse(a.equals(plan(meadow(), List.of(TREE, WIDE), ScatterOptions.DEFAULTS, 100)));
    }

    @Test
    void densityAndSpacingChangeHowMany() {
        int sparse = plan(meadow(), List.of(TREE), ScatterOptions.parse(List.of("density=0.1")), 3).size();
        int dense = plan(meadow(), List.of(TREE), ScatterOptions.parse(List.of("density=1", "spacing=3")), 3).size();
        assertTrue(dense > sparse * 3, sparse + " vs " + dense);
    }

    @Test
    void avoidsRoofsWaterSteepSlopesAndOtherTrees() {
        BuildFixtures.World w = meadow();
        // A plank roof over the east half, a pond in the north-west, a tree trunk at the south-west.
        for (int x = 1; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                w.set(x, 64, z, "oak_planks");
            }
        }
        for (int x = -30; x <= 0; x++) {
            for (int z = -30; z <= -1; z++) {
                w.set(x, 60, z, "sand");
                w.set(x, 61, z, "water");
            }
        }
        for (int y = 61; y <= 66; y++) {
            w.set(-5, y, 5, "oak_log");
        }
        List<ScatterPlanner.Placement> plan = plan(w, List.of(TREE), ScatterOptions.parse(List.of("density=1")), 7);
        assertFalse(plan.isEmpty());
        for (ScatterPlanner.Placement p : plan) {
            assertTrue(p.x() <= 0, "not on or under the roof: " + p);
            assertTrue(p.z() >= 0, "not in the pond: " + p);
            assertFalse(p.x() == -5 && p.z() == 5, "not inside the tree");
        }
        // Clicking the roof itself: refused normally, allowed with on=any.
        List<ScatterPlanner.Placement> onRoof = ScatterPlanner.plan(w, RULES, List.of(TREE), 10, 64, 0, 8,
                ScatterOptions.parse(List.of("density=1")), 7);
        assertTrue(onRoof.stream().noneMatch(p -> p.y() == 65), "the roof is not natural ground");
        List<ScatterPlanner.Placement> anywhere = ScatterPlanner.plan(w, RULES, List.of(TREE), 10, 64, 0, 8,
                ScatterOptions.parse(List.of("density=1", "on=any")), 7);
        assertTrue(anywhere.stream().anyMatch(p -> p.x() > 0 && p.y() == 65), "on=any allows the roof");
    }

    @Test
    void neverFloatsOnUnevenGround() {
        BuildFixtures.World w = meadow();
        // Terraces: every 4 blocks east the ground is a block higher.
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                int top = 60 + Math.floorDiv(x + 30, 4);
                for (int y = 60; y <= top; y++) {
                    w.set(x, y, z, y == top ? "grass_block" : "dirt");
                }
            }
        }
        for (ScatterPlanner.Placement p : plan(w, List.of(WIDE), ScatterOptions.parse(List.of("density=1")), 5)) {
            for (int[] c : WIDE.base(p.turns(), p.flip())) {
                String below = w.get(p.x() + c[0], p.y() - 1, p.z() + c[1]);
                assertTrue(below != null, "base column " + (p.x() + c[0]) + "," + (p.z() + c[1]) + " floats");
            }
        }
        // Too steep: a 2-blocks-per-block slope is refused with the default 35 degrees.
        BuildFixtures.World steep = new BuildFixtures.World();
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                int top = 60 + 2 * x;
                steep.set(x, top, z, "grass_block");
                steep.set(x, top - 1, z, "dirt");
                steep.set(x, top - 2, z, "dirt");
            }
        }
        assertTrue(ScatterPlanner.plan(steep, RULES, List.of(TREE), 0, 60, 0, 10, ScatterOptions.DEFAULTS, 1)
                .isEmpty());
    }

    @Test
    void footprintTurnsLikeWorldEdit() {
        for (int turns = 0; turns < 4; turns++) {
            for (boolean flip : new boolean[]{false, true}) {
                AffineTransform t = new AffineTransform().rotateY(-90.0 * turns).scale(flip ? -1 : 1, 1, 1);
                List<int[]> ours = WIDE.base(turns, flip);
                for (int i = 0; i < WIDE.base().size(); i++) {
                    int[] c = WIDE.base().get(i);
                    Vector3 v = t.apply(Vector3.at(c[0], 0, c[1]));
                    assertEquals(Math.round(v.x()), ours.get(i)[0], "x, turns " + turns + " flip " + flip);
                    assertEquals(Math.round(v.z()), ours.get(i)[1], "z, turns " + turns + " flip " + flip);
                }
            }
        }
    }

    @Test
    void parsesOptions() {
        ScatterOptions o = ScatterOptions.parse(List.of("spacing=5", "density=0.8", "slope=20", "rotate=0",
                "flip=1", "on=any", "replace=all", "sink=2"));
        assertEquals(new ScatterOptions(5, 0.8, 20, false, true, true, true, 2), o);
        assertThrows(IllegalArgumentException.class, () -> ScatterOptions.parse(List.of("density=3")));
        assertThrows(IllegalArgumentException.class, () -> ScatterOptions.parse(List.of("colour=red")));
        assertThrows(IllegalArgumentException.class, () -> ScatterOptions.parse(List.of("oak")));
    }
}
