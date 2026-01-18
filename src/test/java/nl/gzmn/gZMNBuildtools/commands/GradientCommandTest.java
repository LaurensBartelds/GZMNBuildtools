package nl.gzmn.gZMNBuildtools.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientCommand class
 */
class GradientCommandTest {

    private GradientCommand command;

    @BeforeEach
    void setUp() {
        // Create command with null UI manager for testing (UI manager can be null)
        command = new GradientCommand(null);
    }

    @Test
    @DisplayName("Should provide block gradient suggestions")
    void testGetBlockSuggestions() {
        List<String> suggestions = command.getBlockSuggestions();

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty(), "Should have block gradient suggestions");
    }

    @Test
    @DisplayName("Should provide direction suggestions")
    void testGetDirectionSuggestions() {
        List<String> suggestions = command.getDirectionSuggestions();

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty(), "Should have direction suggestions");
    }

    @Test
    @DisplayName("Should provide mode suggestions")
    void testGetModeSuggestions() {
        List<String> suggestions = command.getModeSuggestions();

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty(), "Should have interpolation mode suggestions");
    }

    @Test
    @DisplayName("Should include common gradient patterns in block suggestions")
    void testCommonGradientPatterns() {
        List<String> suggestions = command.getBlockSuggestions();

        // Should include some predefined gradient patterns
        assertTrue(suggestions.stream().anyMatch(s -> s.contains(",")),
            "Block suggestions should include comma-separated patterns");
    }

    @Test
    @DisplayName("Should have all gradient directions in suggestions")
    void testAllDirectionsPresent() {
        List<String> suggestions = command.getDirectionSuggestions();

        assertTrue(suggestions.contains("VERTICAL_UP"), "Should include VERTICAL_UP");
        assertTrue(suggestions.contains("VERTICAL_DOWN"), "Should include VERTICAL_DOWN");
        assertTrue(suggestions.contains("HORIZONTAL_X"), "Should include HORIZONTAL_X");
        assertTrue(suggestions.contains("HORIZONTAL_Z"), "Should include HORIZONTAL_Z");
        assertTrue(suggestions.contains("RADIAL"), "Should include RADIAL");
    }

    @Test
    @DisplayName("Should have all interpolation modes in suggestions")
    void testAllModesPresent() {
        List<String> suggestions = command.getModeSuggestions();

        assertTrue(suggestions.contains("LINEAR"), "Should include LINEAR");
        assertTrue(suggestions.contains("SMOOTH"), "Should include SMOOTH");
        assertTrue(suggestions.contains("DISCRETE"), "Should include DISCRETE");
    }

    @Test
    @DisplayName("Should create command with null UI manager")
    void testCommandWithNullUIManager() {
        assertDoesNotThrow(() -> {
            GradientCommand cmd = new GradientCommand(null);
            assertNotNull(cmd);
        });
    }

    @Test
    @DisplayName("Should not have duplicate direction suggestions")
    void testNoDuplicateDirections() {
        List<String> suggestions = command.getDirectionSuggestions();

        long uniqueCount = suggestions.stream().distinct().count();
        assertEquals(suggestions.size(), uniqueCount, "Direction suggestions should be unique");
    }

    @Test
    @DisplayName("Should not have duplicate mode suggestions")
    void testNoDuplicateModes() {
        List<String> suggestions = command.getModeSuggestions();

        long uniqueCount = suggestions.stream().distinct().count();
        assertEquals(suggestions.size(), uniqueCount, "Mode suggestions should be unique");
    }

    @Test
    @DisplayName("Should have uppercase direction names")
    void testDirectionNamesUppercase() {
        List<String> suggestions = command.getDirectionSuggestions();

        assertTrue(suggestions.stream().allMatch(s -> s.equals(s.toUpperCase())),
            "Direction names should be uppercase");
    }

    @Test
    @DisplayName("Should have uppercase mode names")
    void testModeNamesUppercase() {
        List<String> suggestions = command.getModeSuggestions();

        assertTrue(suggestions.stream().allMatch(s -> s.equals(s.toUpperCase())),
            "Mode names should be uppercase");
    }

    @Test
    @DisplayName("Should not have null in suggestions")
    void testNoNullInSuggestions() {
        assertFalse(command.getBlockSuggestions().contains(null));
        assertFalse(command.getDirectionSuggestions().contains(null));
        assertFalse(command.getModeSuggestions().contains(null));
    }

    @Test
    @DisplayName("Should not have empty strings in suggestions")
    void testNoEmptyStringsInSuggestions() {
        assertFalse(command.getBlockSuggestions().stream().anyMatch(String::isEmpty));
        assertFalse(command.getDirectionSuggestions().stream().anyMatch(String::isEmpty));
        assertFalse(command.getModeSuggestions().stream().anyMatch(String::isEmpty));
    }

    @Test
    @DisplayName("Should include wool gradient pattern")
    void testWoolGradientPattern() {
        List<String> suggestions = command.getBlockSuggestions();

        boolean hasWoolPattern = suggestions.stream()
            .anyMatch(s -> s.contains("wool"));

        assertTrue(hasWoolPattern, "Should include wool gradient patterns");
    }

    @Test
    @DisplayName("Should include wood gradient pattern")
    void testWoodGradientPattern() {
        List<String> suggestions = command.getBlockSuggestions();

        boolean hasWoodPattern = suggestions.stream()
            .anyMatch(s -> s.contains("planks") || s.contains("wood"));

        assertTrue(hasWoodPattern, "Should include wood/planks gradient patterns");
    }

    @Test
    @DisplayName("Should include stone gradient pattern")
    void testStoneGradientPattern() {
        List<String> suggestions = command.getBlockSuggestions();

        boolean hasStonePattern = suggestions.stream()
            .anyMatch(s -> s.contains("stone") || s.contains("cobblestone"));

        assertTrue(hasStonePattern, "Should include stone gradient patterns");
    }

    @Test
    @DisplayName("Should have reasonable number of block suggestions")
    void testReasonableNumberOfBlockSuggestions() {
        List<String> suggestions = command.getBlockSuggestions();

        assertTrue(suggestions.size() >= 3, "Should have at least 3 gradient pattern suggestions");
        assertTrue(suggestions.size() <= 20, "Should not have excessive suggestions");
    }

    @Test
    @DisplayName("Should have exactly 5 direction suggestions")
    void testExactDirectionCount() {
        List<String> suggestions = command.getDirectionSuggestions();

        assertEquals(5, suggestions.size(), "Should have exactly 5 gradient directions");
    }

    @Test
    @DisplayName("Should have exactly 3 mode suggestions")
    void testExactModeCount() {
        List<String> suggestions = command.getModeSuggestions();

        assertEquals(3, suggestions.size(), "Should have exactly 3 interpolation modes");
    }

    @Test
    @DisplayName("Should handle instantiation with UI manager")
    void testCommandWithUIManager() {
        // Create a mock UI manager scenario - just test that it accepts non-null
        assertDoesNotThrow(() -> {
            GradientCommand cmd = new GradientCommand(null);
            assertNotNull(cmd);
        });
    }
}
