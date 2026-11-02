package nl.gzmn.gZMNBuildtools.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataStoreTest {

    @TempDir
    Path dir;

    private PlayerDataStore store() {
        return new PlayerDataStore(dir.toFile(), Runnable::run, Logger.getLogger("test"));
    }

    @Test
    void valuesSurviveANewStore() throws Exception {
        UUID player = UUID.randomUUID();
        PlayerDataStore first = store();
        first.set(player, "gradient-brush", "current.palette", "aged:stone_bricks");
        first.set(player, "terrain-brushes", "loadouts.hill.type", "raise");
        assertTrue(Files.exists(dir.resolve(player + ".yml")));

        PlayerDataStore second = store();
        assertEquals("aged:stone_bricks", second.get(player, "gradient-brush", "current.palette"));
        assertEquals("raise", second.get(player, "terrain-brushes", "loadouts.hill.type"));
        assertEquals(java.util.Set.of("hill"), second.keys(player, "terrain-brushes", "loadouts"));
    }

    @Test
    void mapsReadBackAsSectionsStraightAway() {
        UUID player = UUID.randomUUID();
        PlayerDataStore store = store();
        store.set(player, "gradient-brush", "loadouts.tower", java.util.Map.of("palette", "auto", "radius", "6"));
        assertEquals(java.util.Set.of("tower"), store.keys(player, "gradient-brush", "loadouts"));
        assertEquals(java.util.Set.of("palette", "radius"), store.keys(player, "gradient-brush", "loadouts.tower"));
        assertEquals("6", store.get(player, "gradient-brush", "loadouts.tower.radius"));
        store.set(player, "gradient-brush", "loadouts.tower", java.util.Map.of("palette", "shades:oak_planks"));
        assertEquals(java.util.Set.of("palette"), store.keys(player, "gradient-brush", "loadouts.tower"),
                "saving again replaces the old entry");
    }

    @Test
    void settingNullRemovesAndMissingDataIsEmpty() {
        UUID player = UUID.randomUUID();
        PlayerDataStore store = store();
        assertNull(store.get(player, "gradient-brush", "current"));
        assertTrue(store.keys(player, "gradient-brush", "loadouts").isEmpty());
        store.set(player, "gradient-brush", "loadouts.a", "x");
        store.set(player, "gradient-brush", "loadouts.a", null);
        assertNull(store().get(player, "gradient-brush", "loadouts.a"));
    }

    @Test
    void aCorruptFileStartsFresh() throws Exception {
        UUID player = UUID.randomUUID();
        Files.writeString(dir.resolve(player + ".yml"), "this: [is: not yaml");
        Logger quiet = Logger.getLogger("quiet");
        quiet.setLevel(java.util.logging.Level.OFF);
        PlayerDataStore store = new PlayerDataStore(dir.toFile(), Runnable::run, quiet);
        assertNull(store.get(player, "gradient-brush", "current"));
        store.set(player, "gradient-brush", "current.palette", "auto");
        assertEquals("auto", new PlayerDataStore(new File(dir.toString()), Runnable::run, quiet)
                .get(player, "gradient-brush", "current.palette"));
    }
}
