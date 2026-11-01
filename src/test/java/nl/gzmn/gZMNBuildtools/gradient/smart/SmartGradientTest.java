package nl.gzmn.gZMNBuildtools.gradient.smart;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nl.gzmn.gZMNBuildtools.gradient.smart.SmartFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class SmartGradientTest {

    private final GradientPainter painter = new GradientPainter(RULES, VARIANTS);

    @Test
    void autoAxisIsUpForTowersAndAlongTheLengthForFloors() {
        assertEquals(GradientAxis.UP, GradientFrame.autoAxis(new Bounds(0, 64, 0, 4, 75, 4)));
        assertEquals(GradientAxis.UP, GradientFrame.autoAxis(new Bounds(0, 64, 0, 40, 68, 0)));
        assertEquals(GradientAxis.X, GradientFrame.autoAxis(new Bounds(0, 64, 0, 30, 64, 5)));
        assertEquals(GradientAxis.Z, GradientFrame.autoAxis(new Bounds(0, 64, 0, 3, 65, 20)));
    }

    @Test
    void frameRunsBottomToTopAndHonoursFixedRange() {
        GradientFrame up = GradientFrame.of(new Bounds(0, 64, 0, 4, 74, 4), GradientAxis.UP, null);
        assertEquals(0, up.t(0, 64, 0), 1e-9);
        assertEquals(1, up.t(0, 74, 0), 1e-9);
        assertEquals(0.5, up.t(0, 69, 0), 1e-9);
        GradientFrame down = GradientFrame.of(new Bounds(0, 64, 0, 4, 74, 4), GradientAxis.DOWN, null);
        assertEquals(1, down.t(0, 64, 0), 1e-9);
        GradientFrame fixed = GradientFrame.of(new Bounds(0, 64, 0, 4, 74, 4), GradientAxis.UP,
                new double[]{0, 100});
        assertEquals(0.69, fixed.t(0, 69, 0), 1e-9);
        GradientFrame radial = GradientFrame.of(new Bounds(-5, 0, -5, 5, 0, 5), GradientAxis.RADIAL, null);
        assertEquals(0, radial.t(0, 0, 0), 1e-9);
        assertEquals(1, radial.t(5, 0, 5), 1e-9);
    }

    @Test
    void paletteBandsWithoutSoftnessAndDithersWithIt() {
        Palette p = Palette.ofBlocks(List.of("a", "b"));
        assertEquals("a", p.blockAt(0, 0.5, 0, 0.6));
        assertEquals("b", p.blockAt(1, 0.5, 0, 0.6));
        assertEquals("a", p.blockAt(0.4, 0.99, 0, 0));
        assertEquals("b", p.blockAt(0.6, 0.01, 0, 0));
        // Half way with full softness: the threshold decides, giving an even mix.
        int b = 0;
        for (int i = 0; i < 1000; i++) {
            if (p.blockAt(0.5, Dither.ORDERED.threshold(i, i * 7, i * 3, 1), 0, 1).equals("b")) {
                b++;
            }
        }
        assertTrue(b > 400 && b < 600, "b=" + b);
    }

    @Test
    void orderedDitherIsEvenlySpreadAndDeterministic() {
        double sum = 0;
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                double t = Dither.ORDERED.threshold(x, y, 0, 42);
                assertTrue(t >= 0 && t < 1);
                sum += t;
            }
        }
        assertEquals(0.5, sum / 400, 0.03);
        assertEquals(Dither.ORDERED.threshold(3, 4, 5, 9), Dither.ORDERED.threshold(3, 4, 5, 9));
    }

    @Test
    void scanFindsTheTowerButNotTerrainOrDetails() {
        World w = tower();
        Palette palette = Palette.ofBlocks(List.of("deepslate_bricks", "stone_bricks"));
        TargetPolicy policy = painter.policy(TargetMode.CLICKED, "stone_bricks", palette, Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        assertEquals(new Bounds(0, 64, 0, 4, 75, 4), scan.bounds());
        assertFalse(scan.truncated());
        assertTrue(scan.contains(2, 72, 4), "mossy bricks belong to the stone brick build");
        assertTrue(scan.contains(1, 75, 0), "stairs belong to the build");
        assertFalse(scan.contains(0, 70, 2), "glass is a detail");
        assertFalse(scan.contains(2, 64, 0), "the door is a detail");
        assertFalse(scan.contains(0, 63, 0), "grass is terrain");
        assertEquals("stone_bricks", scan.mainGroup());
    }

    @Test
    void paintingAppliesTheGradientBottomToTopKeepingShapesAndDetails() {
        World w = tower();
        Palette palette = Palette.ofBlocks(List.of("deepslate_bricks", "polished_andesite"));
        GradientSettings settings = GradientSettings.defaults().withSoftness(0);
        TargetPolicy policy = painter.policy(TargetMode.CLICKED, "stone_bricks", palette, Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        GradientFrame frame = GradientFrame.of(scan.bounds(), settings.axis(), null);
        Map<Long, String> changes = new HashMap<>();
        GradientPainter.Result result = painter.paint(w, scan, policy, palette, frame, settings,
                GradientPainter.StrokeFilter.ALL, (x, y, z, from, to) -> {
                    changes.put(LongHashSet.pack(x, y, z), to);
                    w.set(x, y, z, to);
                });
        assertTrue(result.painted() > 100);
        assertEquals("deepslate_bricks", w.get(0, 64, 0));
        // The stair crown stays stairs, in the top material's stair variant.
        assertEquals("polished_andesite_stairs", w.get(1, 75, 0));
        assertEquals("glass", w.get(0, 70, 2));
        assertEquals("oak_door", w.get(2, 64, 0));
        assertEquals("grass_block", w.get(0, 63, 0));
        assertTrue(changes.values().stream().noneMatch(id -> id.equals("glass")));
    }

    @Test
    void secondStrokeOnPaintedBlocksFindsTheSameBuild() {
        World w = tower();
        Palette palette = Palette.ofBlocks(List.of("deepslate_bricks", "polished_andesite"));
        GradientSettings settings = GradientSettings.defaults();
        TargetPolicy policy = painter.policy(TargetMode.CLICKED, "stone_bricks", palette, Set.of());
        BuildScan first = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        painter.paint(w, first, policy, palette, GradientFrame.of(first.bounds(), GradientAxis.AUTO, null),
                settings, GradientPainter.StrokeFilter.ALL, (x, y, z, from, to) -> w.set(x, y, z, to));
        // Click a block that is now deepslate: the palette's groups keep it part of the build.
        TargetPolicy again = painter.policy(TargetMode.CLICKED, w.get(0, 64, 0), palette, Set.of("stone_bricks"));
        BuildScan second = BuildScanner.scan(w, 0, 64, 0, again, RULES, 100_000, 64);
        assertEquals(first.bounds(), second.bounds());
        assertEquals(first.size(), second.size());
    }

    @Test
    void strokeOnlyPaintsInsideTheBrushSphere() {
        World w = tower();
        Palette palette = Palette.ofBlocks(List.of("deepslate_bricks"));
        TargetPolicy policy = painter.policy(TargetMode.CLICKED, "stone_bricks", palette, Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        painter.paint(w, scan, policy, palette, GradientFrame.of(scan.bounds(), GradientAxis.UP, null),
                GradientSettings.defaults(), GradientPainter.StrokeFilter.sphere(0, 64, 0, 2),
                (x, y, z, from, to) -> w.set(x, y, z, to));
        assertEquals("deepslate_bricks", w.get(0, 64, 0));
        assertEquals("stone_bricks", w.get(0, 70, 0));
    }

    @Test
    void mainModeLeavesAccentMaterialsAlone() {
        World w = tower();
        // Oak trim around the base: an accent material.
        w.set(1, 64, 0, "oak_planks");
        w.set(3, 64, 0, "oak_planks");
        Palette palette = Palette.ofBlocks(List.of("deepslate_bricks"));
        TargetPolicy policy = painter.policy(TargetMode.MAIN, "stone_bricks", palette, Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        assertTrue(scan.contains(1, 64, 0), "the trim is part of the build");
        painter.paint(w, scan, policy, palette, GradientFrame.of(scan.bounds(), GradientAxis.UP, null),
                GradientSettings.defaults(), GradientPainter.StrokeFilter.ALL, (x, y, z, f, t) -> w.set(x, y, z, t));
        assertEquals("oak_planks", w.get(1, 64, 0));
        assertEquals("deepslate_bricks", w.get(0, 64, 0));
    }

    @Test
    void scanStopsAtTheBlockLimit() {
        World w = tower();
        Palette palette = Palette.ofBlocks(List.of("stone"));
        TargetPolicy policy = painter.policy(TargetMode.CLICKED, "stone_bricks", palette, Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 50, 64);
        assertTrue(scan.truncated());
        assertEquals(50, scan.size());
    }
}
