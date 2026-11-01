package nl.gzmn.gZMNBuildtools.modules.weathering.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScanner;
import nl.gzmn.gZMNBuildtools.shared.build.LongHashSet;
import nl.gzmn.gZMNBuildtools.shared.build.TargetPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class WeatheringTest {

    private final Weathering weathering = new Weathering(CATALOG);
    private final BuildPainter painter = new BuildPainter(RULES);

    record Vine(int x, int y, int z, String side) {
    }

    /** A taller solid-walled tower so the statistics are meaningful. */
    private static World tallTower() {
        World w = new World();
        for (int x = -6; x <= 10; x++) {
            for (int z = -6; z <= 10; z++) {
                w.set(x, 63, z, "grass_block");
            }
        }
        for (int y = 64; y <= 93; y++) {
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    if (x == 0 || x == 4 || z == 0 || z == 4) {
                        w.set(x, y, z, "stone_bricks");
                    }
                }
            }
        }
        return w;
    }

    private Map<Long, String> weather(World w, WeatherOptions options, List<Vine> vines) {
        TargetPolicy policy = painter.policy(options.target(), "stone_bricks", List.of(), Set.of());
        BuildScan scan = BuildScanner.scan(w, 0, 64, 0, policy, RULES, 100_000, 64);
        Weathering.Pass pass = weathering.pass(scan, w, options);
        Map<Long, String> changes = new HashMap<>();
        painter.paint(w, scan, policy, BuildPainter.StrokeFilter.ALL, pass.chooser(),
                (x, y, z, from, to) -> changes.put(LongHashSet.pack(x, y, z), to));
        pass.vines(BuildPainter.StrokeFilter.ALL, (x, y, z, side) -> vines.add(new Vine(x, y, z, side)));
        return changes;
    }

    private static int y(long key) {
        return LongHashSet.unpackY(key);
    }

    @Test
    void mossStaysLowCracksSitMidwayAndEdgesWear() {
        Map<Long, String> changes = weather(tallTower(), WeatherOptions.parse("strength=1", 32), new ArrayList<>());
        List<Integer> mossY = changes.entrySet().stream().filter(e -> e.getValue().startsWith("mossy_"))
                .map(e -> y(e.getKey())).toList();
        List<Integer> crackY = changes.entrySet().stream().filter(e -> e.getValue().startsWith("cracked_"))
                .map(e -> y(e.getKey())).toList();
        assertTrue(mossY.size() > 10, "moss: " + mossY.size());
        assertTrue(crackY.size() > 10, "cracks: " + crackY.size());
        double mossAvg = mossY.stream().mapToInt(Integer::intValue).average().orElseThrow();
        double crackAvg = crackY.stream().mapToInt(Integer::intValue).average().orElseThrow();
        assertTrue(mossAvg < 72, "moss should sit low, average y " + mossAvg);
        assertTrue(crackAvg > mossAvg, "cracks sit above the moss");
        long worn = changes.values().stream().filter(v -> v.equals("air") || v.endsWith("_slab")).count();
        assertTrue(worn > 0, "some corners and rims wear away");
        assertTrue(changes.values().stream().allMatch(v -> v.equals("air") || v.endsWith("_slab")
                || v.startsWith("mossy_") || v.startsWith("cracked_")), changes.values().toString());
    }

    @Test
    void vinesHangInAirAgainstTheirWall() {
        World w = tallTower();
        List<Vine> vines = new ArrayList<>();
        weather(w, WeatherOptions.parse("strength=1 vines=2", 32), vines);
        assertFalse(vines.isEmpty());
        for (Vine v : vines) {
            assertNull(w.get(v.x, v.y, v.z), "vines only go into air");
            int[] wall = switch (v.side) {
                case "north" -> new int[]{v.x, v.z - 1};
                case "south" -> new int[]{v.x, v.z + 1};
                case "east" -> new int[]{v.x + 1, v.z};
                default -> new int[]{v.x - 1, v.z};
            };
            assertEquals("stone_bricks", w.get(wall[0], v.y, wall[1]), "vine faces its wall: " + v);
        }
    }

    @Test
    void isDeterministicAndOffAtZeroStrength() {
        WeatherOptions options = WeatherOptions.parse("strength=0.8", 32);
        assertEquals(weather(tallTower(), options, new ArrayList<>()), weather(tallTower(), options, new ArrayList<>()));
        List<Vine> vines = new ArrayList<>();
        assertTrue(weather(tallTower(), WeatherOptions.parse("strength=0", 32), vines).isEmpty());
        assertTrue(vines.isEmpty());
    }

    @Test
    void effectsCanBeSwitchedOffAndOptionsAreValidated() {
        Map<Long, String> onlyMoss = weather(tallTower(),
                WeatherOptions.parse("strength=1 cracks=0 decay=0 vines=0", 32), new ArrayList<>());
        assertTrue(onlyMoss.values().stream().allMatch(v -> v.startsWith("mossy_")));
        assertThrows(IllegalArgumentException.class, () -> WeatherOptions.parse("strength=3", 32));
        assertThrows(IllegalArgumentException.class, () -> WeatherOptions.parse("rain=1", 32));
        assertThrows(IllegalArgumentException.class, () -> WeatherOptions.parse("moss", 32));
    }
}
