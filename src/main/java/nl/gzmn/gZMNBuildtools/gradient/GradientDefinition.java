package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a gradient definition with interpolation between block types
 */
public class GradientDefinition {

    private final List<GradientStop> stops;
    private final GradientDirection direction;
    private final InterpolationMode interpolationMode;

    public enum GradientDirection {
        VERTICAL_UP,    // Y+ direction
        VERTICAL_DOWN,  // Y- direction
        HORIZONTAL_X,   // X+ direction
        HORIZONTAL_Z,   // Z+ direction
        RADIAL          // From center outward
    }

    public enum InterpolationMode {
        LINEAR,         // Simple linear interpolation
        SMOOTH,         // Smooth step interpolation
        DISCRETE        // No interpolation, hard steps
    }

    /**
     * Represents a color/block stop in the gradient
     */
    public static class GradientStop {
        private final BlockType blockType;
        private final double position; // 0.0 to 1.0

        public GradientStop(BlockType blockType, double position) {
            this.blockType = blockType;
            this.position = Math.max(0.0, Math.min(1.0, position));
        }

        public BlockType getBlockType() {
            return blockType;
        }

        public double getPosition() {
            return position;
        }
    }

    public GradientDefinition(List<GradientStop> stops, GradientDirection direction, InterpolationMode interpolationMode) {
        if (stops.size() < 2) {
            throw new IllegalArgumentException("Gradient must have at least 2 stops");
        }
        this.stops = new ArrayList<>(stops);
        this.stops.sort((a, b) -> Double.compare(a.position, b.position));
        this.direction = direction;
        this.interpolationMode = interpolationMode;
    }

    /**
     * Create a simple two-block gradient
     */
    public static GradientDefinition simple(BlockType start, BlockType end, GradientDirection direction) {
        return new GradientDefinition(
            Arrays.asList(
                new GradientStop(start, 0.0),
                new GradientStop(end, 1.0)
            ),
            direction,
            InterpolationMode.LINEAR
        );
    }

    /**
     * Get the block type at a specific position (0.0 to 1.0)
     */
    public BlockType getBlockAt(double position) {
        position = Math.max(0.0, Math.min(1.0, position));

        // Find the two stops we're between
        GradientStop before = stops.get(0);
        GradientStop after = stops.get(stops.size() - 1);

        for (int i = 0; i < stops.size() - 1; i++) {
            if (position >= stops.get(i).position && position <= stops.get(i + 1).position) {
                before = stops.get(i);
                after = stops.get(i + 1);
                break;
            }
        }

        // If at exact position, return that block
        if (position == before.position) {
            return before.blockType;
        }
        if (position == after.position) {
            return after.blockType;
        }

        // Calculate interpolation
        double range = after.position - before.position;
        double localPosition = (position - before.position) / range;

        return interpolateBlock(before.blockType, after.blockType, localPosition);
    }

    /**
     * Interpolate between two block types
     */
    private BlockType interpolateBlock(BlockType start, BlockType end, double t) {
        switch (interpolationMode) {
            case SMOOTH:
                // Smooth step function
                t = t * t * (3 - 2 * t);
                break;
            case DISCRETE:
                // Hard step at midpoint
                return t < 0.5 ? start : end;
            case LINEAR:
            default:
                // Use t as-is
                break;
        }

        // For now, we do a simple threshold-based selection
        // In a more advanced version, you could blend similar blocks
        return t < 0.5 ? start : end;
    }

    public List<GradientStop> getStops() {
        return new ArrayList<>(stops);
    }

    public GradientDirection getDirection() {
        return direction;
    }

    public InterpolationMode getInterpolationMode() {
        return interpolationMode;
    }

    /**
     * Parse a gradient from a string format like "stone,cobblestone,andesite"
     */
    public static GradientDefinition parse(String gradientString, GradientDirection direction, InterpolationMode mode) {
        String[] blockNames = gradientString.split(",");
        List<GradientStop> stops = new ArrayList<>();

        for (int i = 0; i < blockNames.length; i++) {
            String blockName = blockNames[i].trim();
            BlockType blockType = BlockTypes.get(blockName.contains(":") ? blockName : "minecraft:" + blockName);

            if (blockType == null) {
                throw new IllegalArgumentException("Unknown block type: " + blockName);
            }

            double position = blockNames.length > 1 ? (double) i / (blockNames.length - 1) : 0.5;
            stops.add(new GradientStop(blockType, position));
        }

        return new GradientDefinition(stops, direction, mode);
    }
}
