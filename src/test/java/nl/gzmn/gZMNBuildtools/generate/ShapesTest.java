package nl.gzmn.gZMNBuildtools.generate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShapesTest {

    private static List<int[]> collect(java.util.function.Consumer<BlockSink> shape) {
        List<int[]> out = new ArrayList<>();
        shape.accept((x, y, z) -> out.add(new int[]{x, y, z}));
        return out;
    }

    private static void assertUnique(List<int[]> blocks) {
        Set<String> seen = new HashSet<>();
        for (int[] b : blocks) {
            assertTrue(seen.add(b[0] + "," + b[1] + "," + b[2]), "duplicate block " + seen.size());
        }
    }

    @Test
    void archEndsAtBothPointsAndPeaksInTheMiddle() {
        List<int[]> blocks = collect(s -> Shapes.curve(0, 64, 0, 20, 64, 0, 8, 1, s));
        assertUnique(blocks);
        assertTrue(blocks.stream().anyMatch(b -> b[0] == 0 && b[1] == 64));
        assertTrue(blocks.stream().anyMatch(b -> b[0] == 20 && b[1] == 64));
        assertEquals(72, blocks.stream().mapToInt(b -> b[1]).max().orElseThrow());
        assertTrue(blocks.stream().filter(b -> b[1] == 72).allMatch(b -> Math.abs(b[0] - 10) <= 2));
    }

    @Test
    void eachExtraThicknessWidensTheCurve() {
        int one = collect(s -> Shapes.curve(0, 0, 0, 30, 0, 0, 0, 1, s)).size();
        int two = collect(s -> Shapes.curve(0, 0, 0, 30, 0, 0, 0, 2, s)).size();
        int three = collect(s -> Shapes.curve(0, 0, 0, 30, 0, 0, 0, 3, s)).size();
        assertEquals(31, one);
        assertTrue(two > one && three > two);
    }

    @Test
    void cableSagsBelowItsEndpoints() {
        List<int[]> blocks = collect(s -> Shapes.curve(0, 80, 0, 0, 80, 30, -5, 1, s));
        assertEquals(75, blocks.stream().mapToInt(b -> b[1]).min().orElseThrow());
    }

    @Test
    void thickCurveIsWiderThanThinCurve() {
        int thin = collect(s -> Shapes.curve(0, 0, 0, 30, 0, 0, 5, 1, s)).size();
        int thick = collect(s -> Shapes.curve(0, 0, 0, 30, 0, 0, 5, 3, s)).size();
        assertTrue(thick > thin * 3);
    }

    @Test
    void hollowShapesHaveFewerBlocksAndAreSubsets() {
        List<int[]> solid = collect(s -> Shapes.dome(0, 0, 0, 8, false, s));
        List<int[]> hollow = collect(s -> Shapes.dome(0, 0, 0, 8, true, s));
        assertUnique(solid);
        assertTrue(hollow.size() < solid.size() / 2);
        Set<String> solidSet = new HashSet<>();
        solid.forEach(b -> solidSet.add(b[0] + "," + b[1] + "," + b[2]));
        hollow.forEach(b -> assertTrue(solidSet.contains(b[0] + "," + b[1] + "," + b[2])));
        assertTrue(solid.stream().allMatch(b -> b[1] >= 0));
    }

    @Test
    void coneNarrowsTowardsTheTop() {
        List<int[]> blocks = collect(s -> Shapes.cone(0, 0, 0, 5, 10, false, s));
        long base = blocks.stream().filter(b -> b[1] == 0).count();
        long top = blocks.stream().filter(b -> b[1] == 9).count();
        assertTrue(base > top);
        assertTrue(top >= 1);
        assertEquals(9, blocks.stream().mapToInt(b -> b[1]).max().orElseThrow());
    }

    @Test
    void hollowConeStillHasAClosedSurface() {
        // Every column of the solid cone must contain at least one hollow-cone block.
        List<int[]> solid = collect(s -> Shapes.cone(0, 0, 0, 6, 12, false, s));
        List<int[]> hollow = collect(s -> Shapes.cone(0, 0, 0, 6, 12, true, s));
        Set<String> hollowColumns = new HashSet<>();
        hollow.forEach(b -> hollowColumns.add(b[0] + "," + b[2]));
        solid.forEach(b -> assertTrue(hollowColumns.contains(b[0] + "," + b[2])));
        assertTrue(hollow.size() < solid.size());
    }

    @Test
    void torusHasAHoleInTheMiddle() {
        List<int[]> blocks = collect(s -> Shapes.torus(0, 0, 0, 8, 2, false, s));
        assertUnique(blocks);
        assertTrue(blocks.stream().noneMatch(b -> b[0] == 0 && b[2] == 0));
        assertTrue(blocks.stream().anyMatch(b -> b[0] == 8 && b[2] == 0 && b[1] == 0));
    }

    @Test
    void spiralClimbsToFullHeight() {
        List<int[]> blocks = collect(s -> Shapes.spiral(0, 0, 0, 5, 20, 2, 1, s));
        assertUnique(blocks);
        assertEquals(0, blocks.stream().mapToInt(b -> b[1]).min().orElseThrow());
        assertEquals(20, blocks.stream().mapToInt(b -> b[1]).max().orElseThrow());
    }
}
