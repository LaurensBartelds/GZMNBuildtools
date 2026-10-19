package nl.gzmn.gZMNBuildtools.terrain;

import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class TerrainMathTest {

    /** World whose terrain is a heightmap: solid at and below height(x, z). */
    private record HeightmapView(IntBinaryOperator height) implements TerrainView {
        @Override
        public boolean isSolid(int x, int y, int z) {
            return y <= height.applyAsInt(x, z);
        }

        @Override
        public int minY() {
            return -64;
        }

        @Override
        public int maxY() {
            return 319;
        }
    }

    @Test
    void flatTerrainHasZeroSlopeAndCurvature() {
        TerrainView flat = new HeightmapView((x, z) -> 64);
        assertEquals(0.0, TerrainMath.slopeDegrees(flat, 0, 64, 0, 2), 1e-9);
        assertEquals(0.0, TerrainMath.curvature(flat, 0, 64, 0, 3), 1e-9);
    }

    @Test
    void rampOfOneBlockPerBlockIs45Degrees() {
        TerrainView ramp = new HeightmapView((x, z) -> 64 + x);
        assertEquals(45.0, TerrainMath.slopeDegrees(ramp, 10, 74, 0, 2), 1e-9);
    }

    @Test
    void steepRampIsSteeperThanGentleRamp() {
        TerrainView gentle = new HeightmapView((x, z) -> 64 + x / 4);
        TerrainView steep = new HeightmapView((x, z) -> 64 + 3 * x);
        assertTrue(TerrainMath.slopeDegrees(steep, 4, 76, 0, 2)
                > TerrainMath.slopeDegrees(gentle, 4, 65, 0, 2));
        assertTrue(TerrainMath.slopeDegrees(steep, 4, 76, 0, 2) > 70);
    }

    @Test
    void valleyIsConcaveAndRidgeIsConvex() {
        TerrainView valley = new HeightmapView((x, z) -> 64 + Math.abs(x));
        TerrainView ridge = new HeightmapView((x, z) -> 80 - Math.abs(x));
        assertTrue(TerrainMath.curvature(valley, 0, 64, 0, 3) > 0);
        assertTrue(TerrainMath.curvature(ridge, 0, 80, 0, 3) < 0);
    }

    @Test
    void surfaceNearFindsClosestSurfaceInWindow() {
        TerrainView flat = new HeightmapView((x, z) -> 64);
        assertEquals(64, TerrainMath.surfaceNear(flat, 0, 0, 70, 8));
        assertEquals(64, TerrainMath.surfaceNear(flat, 0, 0, 50, 20));
        assertEquals(TerrainMath.NO_SURFACE, TerrainMath.surfaceNear(flat, 0, 0, 100, 8));
    }

    @Test
    void depthCountsSolidBlocksAboveAndIsCapped() {
        TerrainView flat = new HeightmapView((x, z) -> 64);
        assertEquals(0, TerrainMath.depth(flat, 0, 64, 0, 10));
        assertEquals(3, TerrainMath.depth(flat, 0, 61, 0, 10));
        assertEquals(6, TerrainMath.depth(flat, 0, 0, 0, 5));
    }

    @Test
    void skyAccessOnlyAtTopOfColumn() {
        TerrainView flat = new HeightmapView((x, z) -> 64);
        assertTrue(TerrainMath.hasSkyAccess(flat, 0, 64, 0));
        assertTrue(TerrainMath.hasSkyAccess(flat, 0, 90, 0));
        assertFalse(TerrainMath.hasSkyAccess(flat, 0, 63, 0));
    }
}
