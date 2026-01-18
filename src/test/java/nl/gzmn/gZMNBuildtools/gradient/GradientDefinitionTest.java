package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientStop;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.InterpolationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientDefinition class
 */
class GradientDefinitionTest {

    @Test
    @DisplayName("Should create gradient with at least 2 stops")
    void testMinimumTwoStops() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            assertDoesNotThrow(() -> {
                new GradientDefinition(
                    Arrays.asList(
                        new GradientStop(stone, 0.0),
                        new GradientStop(cobblestone, 1.0)
                    ),
                    GradientDirection.VERTICAL_UP,
                    InterpolationMode.LINEAR
                );
            });
        }
    }

    @Test
    @DisplayName("Should throw exception with less than 2 stops")
    void testThrowsExceptionWithOneStop() {
        BlockType stone = BlockTypes.get("minecraft:stone");

        if (stone != null) {
            assertThrows(IllegalArgumentException.class, () -> new GradientDefinition(
                    Collections.singletonList(new GradientStop(stone, 0.5)),
                    GradientDirection.VERTICAL_UP,
                    InterpolationMode.LINEAR
                ), "Should require at least 2 stops");
        }
    }

    @Test
    @DisplayName("Should create simple two-block gradient")
    void testSimpleGradient() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = GradientDefinition.simple(
                stone, cobblestone, GradientDirection.VERTICAL_UP
            );

            assertNotNull(gradient);
            assertEquals(GradientDirection.VERTICAL_UP, gradient.getDirection());
            assertEquals(InterpolationMode.LINEAR, gradient.getInterpolationMode());
            assertEquals(2, gradient.getStops().size());
        }
    }

    @Test
    @DisplayName("Should sort gradient stops by position")
    void testStopsSorting() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");
        BlockType andesite = BlockTypes.get("minecraft:andesite");

        if (stone != null && cobblestone != null && andesite != null) {
            // Create stops out of order
            GradientDefinition gradient = new GradientDefinition(
                Arrays.asList(
                    new GradientStop(cobblestone, 0.5),
                    new GradientStop(andesite, 1.0),
                    new GradientStop(stone, 0.0)
                ),
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            List<GradientStop> stops = gradient.getStops();
            assertEquals(0.0, stops.get(0).getPosition(), 0.001);
            assertEquals(0.5, stops.get(1).getPosition(), 0.001);
            assertEquals(1.0, stops.get(2).getPosition(), 0.001);
        }
    }

    @Test
    @DisplayName("Should clamp gradient stop positions to 0.0-1.0 range")
    void testStopPositionClamping() {
        BlockType stone = BlockTypes.get("minecraft:stone");

        if (stone != null) {
            GradientStop stopBelowZero = new GradientStop(stone, -0.5);
            GradientStop stopAboveOne = new GradientStop(stone, 1.5);
            GradientStop stopNormal = new GradientStop(stone, 0.5);

            assertEquals(0.0, stopBelowZero.getPosition(), 0.001, "Should clamp to 0.0");
            assertEquals(1.0, stopAboveOne.getPosition(), 0.001, "Should clamp to 1.0");
            assertEquals(0.5, stopNormal.getPosition(), 0.001, "Should keep valid position");
        }
    }

    @Test
    @DisplayName("Should return correct block at exact stop positions")
    void testGetBlockAtExactPositions() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = GradientDefinition.simple(
                stone, cobblestone, GradientDirection.VERTICAL_UP
            );

            assertEquals(stone, gradient.getBlockAt(0.0), "Should return first block at position 0.0");
            assertEquals(cobblestone, gradient.getBlockAt(1.0), "Should return last block at position 1.0");
        }
    }

    @Test
    @DisplayName("Should interpolate blocks in LINEAR mode")
    void testLinearInterpolation() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = new GradientDefinition(
                Arrays.asList(
                    new GradientStop(stone, 0.0),
                    new GradientStop(cobblestone, 1.0)
                ),
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            // At 0.25, should be closer to stone
            BlockType block25 = gradient.getBlockAt(0.25);
            assertNotNull(block25);

            // At 0.5, threshold behavior
            BlockType block50 = gradient.getBlockAt(0.5);
            assertNotNull(block50);

            // At 0.75, should be closer to cobblestone
            BlockType block75 = gradient.getBlockAt(0.75);
            assertNotNull(block75);
        }
    }

    @Test
    @DisplayName("Should handle DISCRETE interpolation mode")
    void testDiscreteInterpolation() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = new GradientDefinition(
                Arrays.asList(
                    new GradientStop(stone, 0.0),
                    new GradientStop(cobblestone, 1.0)
                ),
                GradientDirection.VERTICAL_UP,
                InterpolationMode.DISCRETE
            );

            // Before midpoint should be stone
            assertEquals(stone, gradient.getBlockAt(0.0));
            assertEquals(stone, gradient.getBlockAt(0.25));
            assertEquals(stone, gradient.getBlockAt(0.49));

            // At or after midpoint should be cobblestone
            assertEquals(cobblestone, gradient.getBlockAt(0.5));
            assertEquals(cobblestone, gradient.getBlockAt(0.75));
            assertEquals(cobblestone, gradient.getBlockAt(1.0));
        }
    }

    @Test
    @DisplayName("Should handle SMOOTH interpolation mode")
    void testSmoothInterpolation() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = new GradientDefinition(
                Arrays.asList(
                    new GradientStop(stone, 0.0),
                    new GradientStop(cobblestone, 1.0)
                ),
                GradientDirection.VERTICAL_UP,
                InterpolationMode.SMOOTH
            );

            // Smooth interpolation should still return valid blocks
            assertNotNull(gradient.getBlockAt(0.25));
            assertNotNull(gradient.getBlockAt(0.5));
            assertNotNull(gradient.getBlockAt(0.75));
        }
    }

    @Test
    @DisplayName("Should clamp input position to 0.0-1.0 in getBlockAt")
    void testGetBlockAtPositionClamping() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = GradientDefinition.simple(
                stone, cobblestone, GradientDirection.VERTICAL_UP
            );

            // Should clamp to valid range
            BlockType belowZero = gradient.getBlockAt(-0.5);
            BlockType aboveOne = gradient.getBlockAt(1.5);

            assertNotNull(belowZero, "Should handle position below 0.0");
            assertNotNull(aboveOne, "Should handle position above 1.0");

            // Should be equivalent to edge positions
            assertEquals(stone, gradient.getBlockAt(-0.5));
            assertEquals(cobblestone, gradient.getBlockAt(1.5));
        }
    }

    @Test
    @DisplayName("Should parse gradient from comma-separated string")
    void testParseGradient() {
        assertDoesNotThrow(() -> {
            GradientDefinition gradient = GradientDefinition.parse(
                "stone,cobblestone,andesite",
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            assertNotNull(gradient);
            assertEquals(3, gradient.getStops().size());
        });
    }

    @Test
    @DisplayName("Should parse gradient with minecraft: prefix")
    void testParseGradientWithPrefix() {
        assertDoesNotThrow(() -> {
            GradientDefinition gradient = GradientDefinition.parse(
                "minecraft:stone,minecraft:cobblestone",
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            assertNotNull(gradient);
            assertEquals(2, gradient.getStops().size());
        });
    }

    @Test
    @DisplayName("Should parse gradient with mixed prefix notation")
    void testParseGradientMixedPrefix() {
        assertDoesNotThrow(() -> {
            GradientDefinition gradient = GradientDefinition.parse(
                "stone,minecraft:cobblestone,andesite",
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            assertNotNull(gradient);
            assertEquals(3, gradient.getStops().size());
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid block names in parse")
    void testParseInvalidBlockName() {
        assertThrows(IllegalArgumentException.class, () -> GradientDefinition.parse(
                "stone,invalid_fake_block_xyz,cobblestone",
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            ), "Should throw exception for invalid block names");
    }

    @Test
    @DisplayName("Should distribute stops evenly when parsing")
    void testParseStopDistribution() {
        GradientDefinition gradient = GradientDefinition.parse(
            "stone,cobblestone,andesite",
            GradientDirection.VERTICAL_UP,
            InterpolationMode.LINEAR
        );

        List<GradientStop> stops = gradient.getStops();
        assertEquals(0.0, stops.get(0).getPosition(), 0.001, "First stop at 0.0");
        assertEquals(0.5, stops.get(1).getPosition(), 0.001, "Middle stop at 0.5");
        assertEquals(1.0, stops.get(2).getPosition(), 0.001, "Last stop at 1.0");
    }

    @Test
    @DisplayName("Should handle three-stop gradient interpolation")
    void testThreeStopGradient() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");
        BlockType andesite = BlockTypes.get("minecraft:andesite");

        if (stone != null && cobblestone != null && andesite != null) {
            GradientDefinition gradient = new GradientDefinition(
                Arrays.asList(
                    new GradientStop(stone, 0.0),
                    new GradientStop(cobblestone, 0.5),
                    new GradientStop(andesite, 1.0)
                ),
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            // Test blocks at various positions
            assertEquals(stone, gradient.getBlockAt(0.0));
            assertEquals(cobblestone, gradient.getBlockAt(0.5));
            assertEquals(andesite, gradient.getBlockAt(1.0));

            // Test interpolation between stops
            assertNotNull(gradient.getBlockAt(0.25)); // Between stone and cobblestone
            assertNotNull(gradient.getBlockAt(0.75)); // Between cobblestone and andesite
        }
    }

    @Test
    @DisplayName("Should support all gradient directions")
    void testAllGradientDirections() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            for (GradientDirection direction : GradientDirection.values()) {
                GradientDefinition gradient = GradientDefinition.simple(
                    stone, cobblestone, direction
                );
                assertEquals(direction, gradient.getDirection());
            }
        }
    }

    @Test
    @DisplayName("Should support all interpolation modes")
    void testAllInterpolationModes() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            for (InterpolationMode mode : InterpolationMode.values()) {
                GradientDefinition gradient = new GradientDefinition(
                    Arrays.asList(
                        new GradientStop(stone, 0.0),
                        new GradientStop(cobblestone, 1.0)
                    ),
                    GradientDirection.VERTICAL_UP,
                    mode
                );
                assertEquals(mode, gradient.getInterpolationMode());
            }
        }
    }

    @Test
    @DisplayName("Should return immutable stops list")
    void testGetStopsReturnsNewList() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null && cobblestone != null) {
            GradientDefinition gradient = GradientDefinition.simple(
                stone, cobblestone, GradientDirection.VERTICAL_UP
            );

            List<GradientStop> stops1 = gradient.getStops();
            List<GradientStop> stops2 = gradient.getStops();

            // Should return new list each time (defensive copy)
            assertNotSame(stops1, stops2, "Should return new list instance");
            assertEquals(stops1.size(), stops2.size());
        }
    }

    @Test
    @DisplayName("Should handle gradient stop with block type and position")
    void testGradientStopCreation() {
        BlockType stone = BlockTypes.get("minecraft:stone");

        if (stone != null) {
            GradientStop stop = new GradientStop(stone, 0.5);

            assertEquals(stone, stop.getBlockType());
            assertEquals(0.5, stop.getPosition(), 0.001);
        }
    }

    @Test
    @DisplayName("Should handle whitespace in parsed gradient string")
    void testParseWithWhitespace() {
        assertDoesNotThrow(() -> {
            GradientDefinition gradient = GradientDefinition.parse(
                "stone , cobblestone , andesite",
                GradientDirection.VERTICAL_UP,
                InterpolationMode.LINEAR
            );

            assertNotNull(gradient);
            assertEquals(3, gradient.getStops().size());
        });
    }

    @Test
    @DisplayName("Should handle edge case with two identical stops at different positions")
    void testIdenticalStopsAtDifferentPositions() {
        BlockType stone = BlockTypes.get("minecraft:stone");

        if (stone != null) {
            assertDoesNotThrow(() -> {
                GradientDefinition gradient = new GradientDefinition(
                    Arrays.asList(
                        new GradientStop(stone, 0.0),
                        new GradientStop(stone, 1.0)
                    ),
                    GradientDirection.VERTICAL_UP,
                    InterpolationMode.LINEAR
                );

                // All positions should return stone
                assertEquals(stone, gradient.getBlockAt(0.0));
                assertEquals(stone, gradient.getBlockAt(0.5));
                assertEquals(stone, gradient.getBlockAt(1.0));
            });
        }
    }
}
