package nl.gzmn.gZMNBuildtools.shared.shapes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverTest {

    private static final Splines.RiverShape SHAPE = new Splines.RiverShape(7, 3, 1, 6, 1);

    /** Parts by position; a position must never get two parts. */
    private static Map<List<Integer>, Integer> river(List<int[]> points, Splines.RiverShape shape,
                                                     Splines.RiverFlow[] flow) {
        Map<List<Integer>, Integer> parts = new HashMap<>();
        Splines.RiverFlow f = Splines.river(Splines.sample(points, 3), shape, (x, y, z, part, axis) -> {
            Integer before = parts.put(List.of(x, y, z), part);
            assertEquals(null, before, "two parts at " + x + "," + y + "," + z);
        });
        if (flow != null) {
            flow[0] = f;
        }
        return parts;
    }

    private static Set<Integer> zs(Map<List<Integer>, Integer> parts, int x, int y, int part) {
        Set<Integer> out = new HashSet<>();
        parts.forEach((pos, p) -> {
            if (pos.get(0) == x && pos.get(1) == y && p == part) {
                out.add(pos.get(2));
            }
        });
        return out;
    }

    @Test
    void straightRiverHasARoundedChannelBelowThePoints() {
        Map<List<Integer>, Integer> parts = river(List.of(new int[]{0, 64, 0}, new int[]{40, 64, 0}), SHAPE, null);

        // Water surface one block below the points, seven wide.
        assertEquals(Set.of(-3, -2, -1, 0, 1, 2, 3), zs(parts, 20, 63, Splines.RIVER_WATER));
        assertTrue(zs(parts, 20, 64, Splines.RIVER_WATER).isEmpty(), "no water above the surface level");
        // Deepest in the middle, shallow at the edges.
        assertEquals(Splines.RIVER_BED, parts.get(List.of(20, 60, 0)), "bed three below the surface in the middle");
        assertEquals(Splines.RIVER_BED, parts.get(List.of(20, 62, 3)), "bed just under the water at the edge");
        assertEquals(Splines.RIVER_WATER, parts.get(List.of(20, 61, 0)));
        // Valley cut above the water, banks sloping up beside it.
        assertEquals(Splines.RIVER_CLEAR, parts.get(List.of(20, 64, 0)));
        assertEquals(Splines.RIVER_CLEAR, parts.get(List.of(20, 69, 0)));
        assertFalse(parts.containsKey(List.of(20, 70, 0)), "valley stops at its height");
        assertFalse(parts.containsKey(List.of(20, 64, 4)), "the ground right beside the water stays");
        assertEquals(Splines.RIVER_CLEAR, parts.get(List.of(20, 65, 4)));
        assertEquals(Splines.RIVER_CLEAR, parts.get(List.of(20, 67, 6)));
        assertFalse(parts.containsKey(List.of(20, 66, 6)));
        // Banks beside the channel, for where the ground falls away.
        assertEquals(Splines.RIVER_BANK, parts.get(List.of(20, 63, 4)));
        assertEquals(Splines.RIVER_BANK, parts.get(List.of(20, 60, -4)));
        assertFalse(parts.containsKey(List.of(20, 63, 6)), "banks are only one ring wide");
    }

    @Test
    void flowsDownhillWhicheverWayThePointsWereClicked() {
        Splines.RiverFlow[] flow = new Splines.RiverFlow[1];
        List<int[]> uphill = List.of(new int[]{0, 60, 0}, new int[]{20, 64, 0}, new int[]{40, 70, 0});
        Map<List<Integer>, Integer> parts = river(uphill, SHAPE, flow);
        assertTrue(flow[0].reversed());
        assertEquals(69, flow[0].sourceLevel());
        assertEquals(59, flow[0].mouthLevel());

        // Following the flow (from x=40 to x=0), the water surface never rises.
        int previous = Integer.MAX_VALUE;
        for (int x = 40; x >= 0; x--) {
            int top = Integer.MIN_VALUE;
            for (Map.Entry<List<Integer>, Integer> e : parts.entrySet()) {
                if (e.getKey().get(0) == x && e.getKey().get(2) == 0 && e.getValue() == Splines.RIVER_WATER) {
                    top = Math.max(top, e.getKey().get(1));
                }
            }
            assertTrue(top <= previous, "water rises at x=" + x);
            previous = top;
        }
    }

    @Test
    void aBumpInTheMiddleIsCutThroughInsteadOfClimbed() {
        Splines.RiverFlow[] flow = new Splines.RiverFlow[1];
        List<int[]> overHill = List.of(new int[]{0, 66, 0}, new int[]{20, 72, 0}, new int[]{40, 64, 0});
        Map<List<Integer>, Integer> parts = river(overHill, SHAPE, flow);
        assertFalse(flow[0].reversed());
        // Under the hilltop the water stays at the source level, with the hill cut away above it.
        assertEquals(Splines.RIVER_WATER, parts.get(List.of(20, 65, 0)));
        assertTrue(zs(parts, 20, 66, Splines.RIVER_WATER).isEmpty());
        assertEquals(Splines.RIVER_CLEAR, parts.get(List.of(20, 71, 0)));
    }

    @Test
    void wideShallowRiverStillHasABedEverywhere() {
        Map<List<Integer>, Integer> parts = river(List.of(new int[]{0, 64, 0}, new int[]{30, 64, 10}),
                new Splines.RiverShape(12, 1, 0, 0, 1), null);
        List<List<Integer>> water = new ArrayList<>();
        parts.forEach((pos, p) -> {
            if (p == Splines.RIVER_WATER) {
                water.add(pos);
            }
        });
        assertFalse(water.isEmpty());
        for (List<Integer> w : water) {
            assertEquals(64, w.get(1), "sink=0 puts the water at the points' level");
            assertEquals(Splines.RIVER_BED, parts.get(List.of(w.get(0), 63, w.get(2))), "bed under " + w);
        }
    }
}
