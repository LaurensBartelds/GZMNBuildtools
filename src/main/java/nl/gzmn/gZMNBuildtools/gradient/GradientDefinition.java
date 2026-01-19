package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GradientDefinition {

    private final List<GradientStop> stops;
    private final GradientDirection direction;
    private final InterpolationMode interpolationMode;

    public enum GradientDirection {
        VERTICAL_UP,
        VERTICAL_DOWN,
        HORIZONTAL_X,
        HORIZONTAL_Z,
        RADIAL
    }

    public enum InterpolationMode {
        LINEAR,
        SMOOTH,
        DISCRETE
    }

    public static class GradientStop {
        private final BlockType blockType;
        private final double position;

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

    public BlockType getBlockAt(double position) {
        position = Math.max(0.0, Math.min(1.0, position));

        GradientStop before = stops.get(0);
        GradientStop after = stops.get(stops.size() - 1);

        for (int i = 0; i < stops.size() - 1; i++) {
            if (position >= stops.get(i).position && position <= stops.get(i + 1).position) {
                before = stops.get(i);
                after = stops.get(i + 1);
                break;
            }
        }

        if (position == before.position) {
            return before.blockType;
        }
        if (position == after.position) {
            return after.blockType;
        }

        double range = after.position - before.position;
        double localPosition = (position - before.position) / range;

        return interpolateBlock(before.blockType, after.blockType, localPosition);
    }

    private BlockType interpolateBlock(BlockType start, BlockType end, double t) {
        switch (interpolationMode) {
            case SMOOTH:
                t = t * t * (3 - 2 * t);
                break;
            case DISCRETE:
                return t < 0.5 ? start : end;
            case LINEAR:
            default:
                break;
        }

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

    /**
     * Create a GradientDefinition from a preset.
     *
     * @param preset The gradient preset to use
     * @param direction The direction for the gradient
     * @return A new GradientDefinition based on the preset
     */
    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction) {
        return parse(preset.getBlocksString(), direction, InterpolationMode.LINEAR);
    }

    /**
     * Create a GradientDefinition from a preset with custom interpolation mode.
     *
     * @param preset The gradient preset to use
     * @param direction The direction for the gradient
     * @param mode The interpolation mode
     * @return A new GradientDefinition based on the preset
     */
    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction, InterpolationMode mode) {
        return parse(preset.getBlocksString(), direction, mode);
    }
}
