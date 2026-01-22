package nl.gzmn.gZMNBuildtools.gradient.storage;

import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset.PresetCategory;
import nl.gzmn.gZMNBuildtools.gradient.model.SavedGradient;
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

        
        storageManager.saveGradient(savedGradient);

        
        GradientStorageManager newManager = new GradientStorageManager(plugin);
        
        
        
        newManager.load();

        
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
        
        UUID authorId = UUID.randomUUID();
        SavedGradient savedGradient = createTestGradient(authorId, "g1");
        storageManager.saveGradient(savedGradient);

        
        storageManager.deleteGradient(authorId, savedGradient.getId());

        
        assertTrue(storageManager.getPlayerGradients(authorId).isEmpty());

        
        GradientStorageManager newManager = new GradientStorageManager(plugin);
        newManager.load();
        assertTrue(newManager.getPlayerGradients(authorId).isEmpty());
    }

    @Test
    void globalGradients_shouldSeparateFromPlayer() {
        
        UUID authorId = UUID.randomUUID();
        SavedGradient privateGradient = createTestGradient(authorId, "private");

        
        GradientPreset pubPreset = new GradientPreset.Builder("public")
                .blocks(java.util.Arrays.asList("minecraft:dirt", "minecraft:grass_block"))
                .category(PresetCategory.CUSTOM)
                .build();
        SavedGradient publicGradient = new SavedGradient("public", "Public", authorId, "User", pubPreset, true,
                System.currentTimeMillis());

        
        storageManager.saveGradient(privateGradient);
        storageManager.saveGradient(publicGradient);

        
        
        
        
        

        
        List<SavedGradient> playerList = storageManager.getPlayerGradients(authorId);
        List<SavedGradient> globalList = storageManager.getGlobalGradients();

        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("private")));
        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("public"))); 

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
