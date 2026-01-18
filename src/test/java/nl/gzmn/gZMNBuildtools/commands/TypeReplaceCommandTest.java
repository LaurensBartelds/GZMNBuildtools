package nl.gzmn.gZMNBuildtools.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeReplaceCommand class
 */
class TypeReplaceCommandTest {

    private TypeReplaceCommand command;

    @BeforeEach
    void setUp() {
        command = new TypeReplaceCommand();
    }

    @Test
    @DisplayName("Should provide material suggestions")
    void testGetSuggestions() {
        List<String> suggestions = command.getSuggestions();

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty(), "Should have material suggestions");
    }

    @Test
    @DisplayName("Should have sorted suggestions")
    void testSuggestionsAreSorted() {
        List<String> suggestions = command.getSuggestions();

        // Check if list is sorted
        for (int i = 0; i < suggestions.size() - 1; i++) {
            String current = suggestions.get(i);
            String next = suggestions.get(i + 1);
            assertTrue(current.compareTo(next) <= 0,
                "Suggestions should be sorted alphabetically: " + current + " vs " + next);
        }
    }

    @Test
    @DisplayName("Should include common materials in suggestions")
    void testCommonMaterialsInSuggestions() {
        List<String> suggestions = command.getSuggestions();

        // Check for some common materials that should be present
        assertTrue(suggestions.contains("stone") || suggestions.contains("cobblestone"),
            "Should include common stone materials");
        assertTrue(suggestions.contains("oak") || suggestions.contains("wood"),
            "Should include wood materials");
    }

    @Test
    @DisplayName("Should have unique suggestions")
    void testSuggestionsAreUnique() {
        List<String> suggestions = command.getSuggestions();

        long uniqueCount = suggestions.stream().distinct().count();
        assertEquals(suggestions.size(), uniqueCount, "All suggestions should be unique");
    }

    @Test
    @DisplayName("Should handle copper materials in suggestions")
    void testCopperMaterialsInSuggestions() {
        List<String> suggestions = command.getSuggestions();

        // Copper or copper groups should be present
        boolean hasCopper = suggestions.stream()
            .anyMatch(s -> s.contains("copper"));

        assertTrue(hasCopper, "Should include copper-related materials");
    }

    @Test
    @DisplayName("Should not have null suggestions")
    void testNoNullSuggestions() {
        List<String> suggestions = command.getSuggestions();

        assertFalse(suggestions.contains(null), "Suggestions should not contain null values");
    }

    @Test
    @DisplayName("Should not have empty string suggestions")
    void testNoEmptyStringSuggestions() {
        List<String> suggestions = command.getSuggestions();

        assertFalse(suggestions.contains(""), "Suggestions should not contain empty strings");
        assertTrue(suggestions.stream().noneMatch(s -> s.trim().isEmpty()),
            "Suggestions should not contain blank strings");
    }

    @Test
    @DisplayName("Should create command instance without errors")
    void testCommandInstantiation() {
        assertDoesNotThrow(() -> {
            TypeReplaceCommand cmd = new TypeReplaceCommand();
            assertNotNull(cmd);
        });
    }

    @Test
    @DisplayName("Should handle case variations in material names")
    void testCaseInsensitivity() {
        List<String> suggestions = command.getSuggestions();

        // All suggestions should be lowercase for consistency
        assertTrue(suggestions.stream().allMatch(s -> s.equals(s.toLowerCase())),
            "Material suggestions should be lowercase");
    }

    @Test
    @DisplayName("Should include material groups if available")
    void testMaterialGroupsInSuggestions() {
        List<String> suggestions = command.getSuggestions();

        // Material groups might start with "all_" or "copper_all" etc.
        // Just verify the method returns consistently
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
    }
}
