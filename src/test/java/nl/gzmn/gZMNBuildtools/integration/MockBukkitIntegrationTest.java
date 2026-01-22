package nl.gzmn.gZMNBuildtools.integration;

import nl.gzmn.gZMNBuildtools.commands.GradientCommand;
import nl.gzmn.gZMNBuildtools.gradient.GradientPreset;
import nl.gzmn.gZMNBuildtools.gradient.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.gradient.SavedGradient;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for GZMNBuildtools plugin components.
 * 
 * Note: MockBukkit 4.0.0 has compatibility issues with Paper 1.21.3's
 * RegistryAccess.
 * These tests use Mockito mocks for Player objects instead of MockBukkit's
 * PlayerMock
 * to avoid the RegistryAccessMock initialization failures.
 * 
 * When MockBukkit releases a version compatible with Paper 1.21.3, these tests
 * can be
 * updated to use the full MockBukkit server simulation.
 */
class MockBukkitIntegrationTest {

    private Path tempDir;
    private Plugin mockPlugin;

    @BeforeEach
    void setUp() throws Exception {
        // Create a temp directory for file-based tests
        tempDir = Files.createTempDirectory("gzmn-test");

        // Create a mock plugin for tests
        mockPlugin = mock(Plugin.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Cleanup temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void gradientCommand_withMockPlayer_shouldHandleNoUIGracefully() {
        // Create a mock player using Mockito (avoiding MockBukkit's PlayerMock)
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // Create command without UI manager
        GradientCommand cmd = new GradientCommand(null);

        // Execute - should handle gracefully
        cmd.openUI(player);

        // Verify player received an error message
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(captor.capture());

        // Should have sent a message
        assertNotNull(captor.getValue());
    }

    @Test
    void gradientCommand_invalidDirection_shouldSendError() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        GradientCommand cmd = new GradientCommand(null);

        // Execute with invalid direction
        cmd.execute(player, "stone,dirt", "INVALID_DIR", "LINEAR");

        // Verify error message was sent
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeast(1)).sendMessage(captor.capture());
        assertFalse(captor.getAllValues().isEmpty());
    }

    @Test
    void gradientStorageManager_shouldPersistAndReload() {
        // Create storage manager
        GradientStorageManager storageManager = new GradientStorageManager(mockPlugin);

        // Create and save a gradient
        UUID playerUuid = UUID.randomUUID();
        GradientPreset preset = new GradientPreset.Builder("integration_test_1")
                .displayName("Integration Test Gradient")
                .description("Created during integration test")
                .category(GradientPreset.PresetCategory.CUSTOM)
                .blocks(Arrays.asList("minecraft:stone", "minecraft:cobblestone", "minecraft:gravel"))
                .build();

        SavedGradient saved = new SavedGradient(
                "integration_test_1",
                "Integration Test",
                playerUuid,
                "TestPlayer",
                preset,
                false,
                System.currentTimeMillis());

        storageManager.saveGradient(saved);

        // Create a new manager instance (simulates server restart)
        GradientStorageManager reloadedManager = new GradientStorageManager(mockPlugin);
        reloadedManager.load();

        // Verify data persisted
        List<SavedGradient> loaded = reloadedManager.getPlayerGradients(playerUuid);
        assertEquals(1, loaded.size());
        assertEquals("integration_test_1", loaded.get(0).getId());
        assertEquals("Integration Test", loaded.get(0).getName());
    }

    @Test
    void gradientStorageManager_publicGradients_shouldBeSeparatedFromPlayer() {
        GradientStorageManager storageManager = new GradientStorageManager(mockPlugin);

        UUID authorId = UUID.randomUUID();

        // Create a private gradient
        GradientPreset privatePreset = new GradientPreset.Builder("private_grad")
                .displayName("Private Gradient")
                .category(GradientPreset.PresetCategory.CUSTOM)
                .blocks(Arrays.asList("minecraft:stone", "minecraft:dirt"))
                .build();
        SavedGradient privateGradient = new SavedGradient(
                "private_grad", "Private", authorId, "User", privatePreset, false, System.currentTimeMillis());

        // Create a public gradient
        GradientPreset publicPreset = new GradientPreset.Builder("public_grad")
                .displayName("Public Gradient")
                .category(GradientPreset.PresetCategory.CUSTOM)
                .blocks(Arrays.asList("minecraft:oak_planks", "minecraft:dark_oak_planks"))
                .build();
        SavedGradient publicGradient = new SavedGradient(
                "public_grad", "Public", authorId, "User", publicPreset, true, System.currentTimeMillis());

        // Save both
        storageManager.saveGradient(privateGradient);
        storageManager.saveGradient(publicGradient);

        // Verify separation
        List<SavedGradient> playerList = storageManager.getPlayerGradients(authorId);
        List<SavedGradient> globalList = storageManager.getGlobalGradients();

        // Player should have both
        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("private_grad")));
        assertTrue(playerList.stream().anyMatch(g -> g.getId().equals("public_grad")));

        // Global should only have public
        assertTrue(globalList.stream().anyMatch(g -> g.getId().equals("public_grad")));
        assertFalse(globalList.stream().anyMatch(g -> g.getId().equals("private_grad")));
    }

    @Test
    void gradientStorageManager_deleteGradient_shouldRemoveFromStorage() {
        GradientStorageManager storageManager = new GradientStorageManager(mockPlugin);

        UUID authorId = UUID.randomUUID();
        GradientPreset preset = new GradientPreset.Builder("to_delete")
                .category(GradientPreset.PresetCategory.CUSTOM)
                .blocks(Arrays.asList("minecraft:stone", "minecraft:cobblestone"))
                .build();
        SavedGradient gradient = new SavedGradient(
                "to_delete", "To Delete", authorId, "User", preset, false, System.currentTimeMillis());

        storageManager.saveGradient(gradient);
        assertEquals(1, storageManager.getPlayerGradients(authorId).size());

        // Delete
        storageManager.deleteGradient(authorId, "to_delete");

        // Verify deletion
        assertTrue(storageManager.getPlayerGradients(authorId).isEmpty());

        // Verify persistence
        GradientStorageManager reloaded = new GradientStorageManager(mockPlugin);
        reloaded.load();
        assertTrue(reloaded.getPlayerGradients(authorId).isEmpty());
    }

    @Test
    void gradientPreset_builder_shouldCreateValidPreset() {
        GradientPreset preset = new GradientPreset.Builder("test_preset")
                .displayName("Test Display Name")
                .description("Test Description")
                .category(GradientPreset.PresetCategory.STONE)
                .blocks(Arrays.asList("minecraft:stone", "minecraft:cobblestone"))
                .build();

        assertEquals("test_preset", preset.getId());
        assertEquals("Test Display Name", preset.getDisplayName());
        assertEquals("Test Description", preset.getDescription());
        assertEquals(GradientPreset.PresetCategory.STONE, preset.getCategory());
        assertEquals(2, preset.getBlockIds().size());
        assertTrue(preset.getBlockIds().contains("minecraft:stone"));
        assertTrue(preset.getBlockIds().contains("minecraft:cobblestone"));
    }
}
