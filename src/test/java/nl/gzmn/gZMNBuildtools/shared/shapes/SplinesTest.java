package nl.gzmn.gZMNBuildtools.shared.shapes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SplinesTest {

    private static final List<int[]> POINTS = List.of(
            new int[]{0, 64, 0}, new int[]{10, 66, 4}, new int[]{20, 64, 0}, new int[]{30, 70, -6});

    @Test
    void curvePassesThroughEveryPoint() {
        List<double[]> samples = Splines.sample(POINTS, 4);
        for (int[] p : POINTS) {
            assertTrue(samples.stream().anyMatch(s -> Math.abs(s[0] - p[0]) < 1e-6 && Math.abs(s[1] - p[1]) < 1e-6
                    && Math.abs(s[2] - p[2]) < 1e-6), "missing " + p[0] + "," + p[1] + "," + p[2]);
        }
    }

    @Test
    void curveIsSmoothAndStaysNearItsPoints() {
        List<double[]> samples = Splines.sample(POINTS, 4);
        for (int i = 1; i < samples.size(); i++) {
            double[] a = samples.get(i - 1);
            double[] b = samples.get(i);
            double step = Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
            assertTrue(step < 1.0, "gap of " + step + " blocks");
        }
        // Centripetal splines do not overshoot far beyond the points' box.
        assertTrue(samples.stream().allMatch(s -> s[2] > -9 && s[2] < 7 && s[1] > 62 && s[1] < 72));
    }

    @Test
    void handlesDuplicatesAndSinglePoints() {
        assertEquals(1, Splines.sample(List.of(new int[]{1, 2, 3}), 4).size());
        List<double[]> dup = Splines.sample(List.of(new int[]{0, 0, 0}, new int[]{0, 0, 0}, new int[]{5, 0, 0}), 4);
        assertTrue(dup.stream().noneMatch(s -> Double.isNaN(s[0])));
    }

    @Test
    void hollowTubeIsAShellOfTheSolidTube() {
        List<double[]> samples = Splines.sample(List.of(new int[]{0, 64, 0}, new int[]{20, 64, 0}), 4);
        Set<String> solid = new HashSet<>();
        Splines.tube(samples, 3, false, (x, y, z) -> solid.add(x + "," + y + "," + z));
        List<String> hollow = new ArrayList<>();
        Splines.tube(samples, 3, true, (x, y, z) -> hollow.add(x + "," + y + "," + z));
        assertTrue(solid.containsAll(hollow));
        assertTrue(hollow.size() < solid.size());
        assertFalse(hollow.contains("10,64,0"), "the centre of a hollow tube is open");
    }

    @Test
    void wallAndRoadHaveTheRequestedSize() {
        List<double[]> straight = Splines.sample(List.of(new int[]{0, 64, 0}, new int[]{10, 64, 0}), 4);
        Set<String> wall = new HashSet<>();
        Splines.wall(straight, 5, 1, (x, y, z) -> wall.add(x + "," + y + "," + z));
        assertEquals(11 * 5, wall.size());

        List<int[]> surface = new ArrayList<>();
        List<int[]> cleared = new ArrayList<>();
        List<int[]> filled = new ArrayList<>();
        Splines.road(straight, 3, 2, 1, (x, y, z, part, axis) -> {
            if (part == Splines.ROAD_SURFACE) {
                surface.add(new int[]{x, y, z});
            } else if (part == Splines.ROAD_CLEAR) {
                cleared.add(new int[]{x, y, z});
            } else {
                filled.add(new int[]{x, y, z});
            }
        });
        assertEquals(11 * 3, surface.size());
        assertTrue(surface.stream().allMatch(b -> b[1] == 64 && b[2] >= -1 && b[2] <= 1));
        assertEquals(11 * 3 * 2, cleared.size());
        assertEquals(11 * 3, filled.size());
    }

    @Test
    void dashesAlternateAlongTheCurve() {
        List<double[]> straight = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            straight.add(new double[]{i, 64, 0});
        }
        Set<String> placed = new HashSet<>();
        Splines.dashes(straight, 3, 2, 1, (x, y, z) -> placed.add(x + "," + y + "," + z));
        // period 5: samples 0-2, 5-7, 10-12, 15-17, 20 are on
        for (int i : new int[]{0, 1, 2, 5, 6, 7, 10, 11, 12, 15, 16, 17, 20}) {
            assertTrue(placed.contains(i + ",64,0"), "missing dash block at x=" + i);
        }
        for (int i : new int[]{3, 4, 8, 9, 13, 14, 18, 19}) {
            assertFalse(placed.contains(i + ",64,0"), "unexpected gap block at x=" + i);
        }
        assertEquals(13, placed.size());
    }

    @Test
    void zeroGapDashesAreASolidLine() {
        List<double[]> straight = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            straight.add(new double[]{i, 64, 0});
        }
        Set<String> placed = new HashSet<>();
        Splines.dashes(straight, 3, 0, 1, (x, y, z) -> placed.add(x + "," + y + "," + z));
        assertEquals(11, placed.size());
    }

    @Test
    void dashesStampTheRequestedWidthAcrossTheCurve() {
        List<double[]> straight = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            straight.add(new double[]{i, 64, 0});
        }
        Set<String> placed = new HashSet<>();
        Splines.dashes(straight, 11, 0, 3, (x, y, z) -> placed.add(x + "," + y + "," + z));
        assertEquals(11 * 3, placed.size());
        assertTrue(placed.contains("5,64,-1") && placed.contains("5,64,1"));
    }
}
