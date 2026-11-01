package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GradientSettingsStoreTest {

    @Test
    void settingsRoundTripThroughTheirSavedForm() {
        GradientSettings s = GradientSettings.defaults().withPalette("aged:stone_bricks").withSteps(6)
                .withAxis(GradientAxis.DOWN).withTarget(nl.gzmn.gZMNBuildtools.shared.build.TargetMode.MAIN).withDither(Dither.NOISE)
                .withSoftness(0.3).withKeepShapes(false).withRange(new double[]{60, 120}).withRadius(9);
        GradientSettings back = GradientSettings.fromMap(s.describe(), 32, p -> true);
        assertEquals(s.describe(), back.describe());
        assertEquals(GradientSettings.defaults().describe(),
                GradientSettings.fromMap(GradientSettings.defaults().describe(), 32, p -> true).describe());
    }

    @Test
    void brokenSavedValuesFallBackToDefaults() {
        Map<String, Object> saved = Map.of("palette", "stone,removed_block", "axis", "sideways", "radius", "500",
                "softness", "0.2");
        GradientSettings s = GradientSettings.fromMap(saved, 32, p -> !p.contains("removed"));
        assertEquals("auto", s.palette());
        assertEquals(GradientAxis.AUTO, s.axis());
        assertEquals(4, s.radius());
        assertEquals(0.2, s.softness(), 1e-9);
    }

    @Test
    void storeLoadsLazilyAndSavesEveryChange() {
        Map<UUID, GradientSettings> disk = new HashMap<>();
        GradientSettingsStore.Persistence persistence = new GradientSettingsStore.Persistence() {
            public Optional<GradientSettings> load(UUID player) {
                return Optional.ofNullable(disk.get(player));
            }

            public void save(UUID player, GradientSettings settings) {
                if (settings == null) {
                    disk.remove(player);
                } else {
                    disk.put(player, settings);
                }
            }
        };
        UUID player = UUID.randomUUID();
        disk.put(player, GradientSettings.defaults().withPalette("shades:oak_planks"));
        GradientSettingsStore store = new GradientSettingsStore(persistence);
        assertEquals("shades:oak_planks", store.get(player).palette());
        store.update(player, s -> s.withRadius(7));
        assertEquals(7, disk.get(player).radius());
        store.reset(player);
        assertFalse(disk.containsKey(player));
        assertEquals("auto", store.get(player).palette());
    }
}
