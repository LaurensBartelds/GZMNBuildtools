package nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine;

import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.SnowSmooth.Cell;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.SnowSmooth.ColumnSnow;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.SnowSmooth.ColumnTop;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.SnowSmooth.Plan;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowSmoothTest {

    private static final ColumnTop FLAT = (x, z) -> 64;

    private static ColumnSnow snow(Map<Long, Integer> layers) {
        return (x, z) -> layers.getOrDefault(((long) x << 32) | (z & 0xffffffffL), 0);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    @Test
    void uniformSnowStaysUnchanged() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                layers.put(key(x, z), 4);
            }
        }
        Plan plan = SnowSmooth.plan(0, 0, 2, FLAT, snow(layers), 1.0, false, -64, 320);
        assertEquals(0, plan.blockCount());
        assertTrue(plan.newLayers().isEmpty());
        assertTrue(plan.remove().isEmpty());
    }

    @Test
    void spikeIsAveragedDown() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        layers.put(key(0, 0), 8);
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(layers), 1.0, false, -64, 320);
        // average over the 3x3 disc is 8/9, so every cell moves to 1 layer
        assertEquals(1, plan.newLayers().get(new Cell(0, 65, 0)));
        assertEquals(9, plan.blockCount());
        assertEquals(1, plan.newLayers().get(new Cell(1, 65, 0)));
    }

    @Test
    void partialStrengthMovesHalfway() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        layers.put(key(0, 0), 8);
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(layers), 0.5, false, -64, 320);
        // 8 + (8/9 - 8) * 0.5 = 4.44 -> 4
        assertEquals(4, plan.newLayers().get(new Cell(0, 65, 0)));
    }

    @Test
    void thinSnowAveragingToZeroIsRemoved() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        layers.put(key(0, 0), 1);
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(layers), 1.0, false, -64, 320);
        assertTrue(plan.remove().contains(new Cell(0, 65, 0)));
        assertTrue(plan.newLayers().isEmpty());
        assertEquals(1, plan.blockCount());
    }

    @Test
    void fillLaysSnowOnBareGroundNearThickSnow() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    layers.put(key(x, z), 8);
                }
            }
        }
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(layers), 1.0, true, -64, 320);
        // centre is bare, average is 64/9 = 7.1 -> gains 7 layers
        assertEquals(7, plan.newLayers().get(new Cell(0, 65, 0)));
    }

    @Test
    void fillDoesNotLaySnowOnIsolatedBareGround() {
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(Map.of()), 1.0, true, -64, 320);
        assertEquals(0, plan.blockCount());
    }

    @Test
    void columnsAtWorldTopAreSkipped() {
        ColumnTop ceiling = (x, z) -> 320;
        Map<Long, Integer> layers = new java.util.HashMap<>();
        layers.put(key(0, 0), 8);
        Plan plan = SnowSmooth.plan(0, 0, 1, ceiling, snow(layers), 1.0, false, -64, 320);
        assertEquals(0, plan.blockCount());
    }

    @Test
    void centreMovesToTheNeighbourhoodAverage() {
        Map<Long, Integer> layers = new java.util.HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                layers.put(key(x, z), 8);
            }
        }
        layers.put(key(0, 0), 3);
        Plan plan = SnowSmooth.plan(0, 0, 1, FLAT, snow(layers), 1.0, false, -64, 320);
        // average (8*8+3)/9 = 7.44 -> 7, and never above the 1..8 range
        assertEquals(7, plan.newLayers().get(new Cell(0, 65, 0)));
    }
}
