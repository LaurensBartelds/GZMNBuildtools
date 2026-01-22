package nl.gzmn.gZMNBuildtools.gradient;

import nl.gzmn.gZMNBuildtools.gradient.GradientPreset.PresetCategory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradientStorageManagerTest {

    @Mock
    private Plugin plugin;

    @TempDir
    Path tempDir;

    private GradientStorageManager storageManager;
    private File dataFolder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataFolder = tempDir.toFile();
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        storageManager = new GradientStorageManager(plugin);
    }

    @Test
    void saveAndLoadGradient_shouldPersistData() {
        // Arrange
        UUID authorId = UUID.randomUUID();
        String authorName = "TestUser";
        GradientPreset preset = new GradientPreset.Builder("test_id")
                .displayName("Test Gradient")
                .description("A test gradient")
                .category(PresetCategory.CUSTOM)
                .blocks(java.util.Arrays.asList("minecraft:stone", "minecraft:dirt"))
                .build();

        SavedGradient savedGradient = new SavedGradient(
                "test_id",
                "Test Gradient",
                authorId,
                authorName,
                preset,
                false,
                System.currentTimeMillis());

        // Act
        storageManager.saveGradient(savedGradient);

        // Simulate reload by creating new manager
        GradientStorageManager newManager = new GradientStorageManager(plugin);
        // Load is called in constructor or explicitly?
        // In GZMNBuildtools it's called explicitly.
        // But constructor initializes files. Need to call load().
        newManager.load();

        // Assert
        List<SavedGradient> loaded = newManager.getPlayerGradients(authorId);
        assertEquals(1, loaded.size());
        SavedGradient loadedGradient = loaded.get(0);
        assertEquals("test_id", loadedGradient.getId());
        assertEquals("Test Gradient", loadedGradient.getName());
        assertEquals(authorId, loadedGradient.getAuthorInfo());
        assertEquals(PresetCategory.CUSTOM, loadedGradient.getPreset().getCategory());
    }

    @Test
    void deleteGradient_shouldRemoveData() {
        // Arrange
        UUID authorId = UUID.randomUUID();
        SavedGradient savedGradient = createTestGradient(authorId, "g1");
        storageManager.saveGradient(savedGradient);

        // Act
        storageManager.deleteGradient(authorId, savedGradient.getId());

        // Assert
        assertTrue(storageManager.getPlayerGradients(authorId).isEmpty());

        // Verify file persistence check
        GradientStorageManager newManager = new GradientStorageManager(plugin);
        newManager.load();
        assertTrue(newManager.getPlayerGradients(authorId).isEmpty());
    }

    @Test
    void globalGradients_shouldSeparateFromPlayer() {
        // Arrange
        UUID authorId = UUID.randomUUID();
        SavedGradient privateGradient = createTestGradient(authorId, "private");

        // Public gradient
        GradientPreset pubPreset = new GradientPreset.Builder("public")
                .blocks(java.util.Arrays.asList("minecraft:dirt", "minecraft:grass_block"))
                .category(PresetCategory.CUSTOM)
                .build();
        SavedGradient publicGradient = new SavedGradient("public", "Public", authorId, "User", pubPreset, true,
                System.currentTimeMillis());

        // Act
        storageManager.saveGradient(privateGradient);
        storageManager.saveGradient(publicGradient);

        // Current implementation: saveGradient adds to player list AND global list if
        // public?
        // Let's check logic. Based on logic:
        // if (gradient.isPublic()) globalGradients.add(...)
        // always add to playerGradients too?

        // Assert
        List<SavedGradient> playerList = storageManager.getPlayerGradients(authorId);
        List<SavedGradient> globalList = storageManager.getGlobalGradients();

        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("private")));
        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("public"))); // Assuming it tracks all authored

        assertTrue(globalList.stream().anyMatch(g -> g.getId().equals("public")));
        assertFalse(globalList.stream().anyMatch(g -> g.getId().equals("private")));
    }

    private SavedGradient createTestGradient(UUID authorId, String id) {
        GradientPreset preset = new GradientPreset.Builder(id)
                .displayName("Gradient " + id)
                .category(PresetCategory.CUSTOM)
                .blocks(java.util.Arrays.asList("minecraft:stone", "minecraft:cobblestone"))
                .build();
        return new SavedGradient(id, "Gradient " + id, authorId, "User", preset, false, System.currentTimeMillis());
    }
}
