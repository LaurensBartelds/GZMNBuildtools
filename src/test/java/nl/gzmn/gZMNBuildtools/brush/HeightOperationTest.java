package nl.gzmn.gZMNBuildtools.brush;

import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class HeightOperationTest {

    private static HeightField field(int radius, IntBinaryOperator height) {
        int size = radius * 2 + 1;
        int[] heights = new int[size * size];
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                heights[(dz + radius) * size + dx + radius] = height.applyAsInt(dx, dz);
            }
        }
        return new HeightField(0, 64, 0, radius, heights);
    }

    @Test
    void falloffIsOneAtCentreAndZeroAtEdge() {
        assertEquals(1.0, HeightOperation.falloff(0, 0), 1e-9);
        assertEquals(0.0, HeightOperation.falloff(1, 0), 1e-9);
        assertEquals(1.0, HeightOperation.falloff(0.4, 0.5), 1e-9);
        assertTrue(HeightOperation.falloff(0.5, 0) < 1);
    }

    @Test
    void raiseIsHighestInTheMiddleAndZeroOutsideCircle() {
        HeightField flat = field(5, (x, z) -> 64);
        int[] d = HeightOperation.raise(4, 0).deltas(flat);
        assertEquals(4, d[flat.index(0, 0)]);
        assertTrue(d[flat.index(3, 0)] < 4);
        assertEquals(0, d[flat.index(5, 5)]);
    }

    @Test
    void lowerIsNegativeRaise() {
        HeightField flat = field(5, (x, z) -> 64);
        assertEquals(-4, HeightOperation.raise(-4, 0).deltas(flat)[flat.index(0, 0)]);
    }

    @Test
    void flattenPullsColumnsToCentreHeight() {
        HeightField bumpy = field(4, (x, z) -> 64 + (x == 1 ? 5 : 0) - (x == -1 ? 3 : 0));
        int[] d = HeightOperation.flatten(1, 0.5).deltas(bumpy);
        assertEquals(-5, d[bumpy.index(1, 0)]);
        assertEquals(3, d[bumpy.index(-1, 0)]);
        assertEquals(0, d[bumpy.index(0, 0)]);
    }

    @Test
    void smoothShrinksASpike() {
        HeightField spike = field(5, (x, z) -> x == 0 && z == 0 ? 74 : 64);
        int[] d = HeightOperation.smooth(2, 2, 1).deltas(spike);
        assertTrue(d[spike.index(0, 0)] < -5, "spike should drop substantially");
        assertTrue(d[spike.index(4, 0)] >= 0);
    }

    @Test
    void terraceSnapsToSteps() {
        HeightField ramp = field(6, (x, z) -> 64 + x);
        int[] d = HeightOperation.terrace(4, 0, 0.9).deltas(ramp);
        // Near the centre the result lands on a multiple of 4.
        assertEquals(0, Math.floorMod(ramp.height(1, 0) + d[ramp.index(1, 0)], 4));
        assertEquals(0, Math.floorMod(ramp.height(-1, 0) + d[ramp.index(-1, 0)], 4));
    }

    @Test
    void columnsWithoutSurfaceAreLeftAlone() {
        HeightField holes = field(3, (x, z) -> x == 1 ? nl.gzmn.gZMNBuildtools.terrain.TerrainMath.NO_SURFACE : 64);
        assertEquals(0, HeightOperation.raise(3, 0).deltas(holes)[holes.index(1, 0)]);
    }
}
