package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        DISCRETE,
        BLENDED
    }

    /**
     * Represents a weighted block type for percentage-based selection.
     */
    public static class WeightedBlock {
        private final BlockType blockType;
        private final double weight;

        public WeightedBlock(BlockType blockType, double weight) {
            this.blockType = blockType;
            this.weight = weight;
        }

        public BlockType getBlockType() {
            return blockType;
        }

        public double getWeight() {
            return weight;
        }
    }

    public static class GradientStop {
        private final List<WeightedBlock> weightedBlocks;
        private final double position;
        private final double totalWeight;

        /**
         * Create a stop with a single block type (weight 1.0).
         */
        public GradientStop(BlockType blockType, double position) {
            this.weightedBlocks = Arrays.asList(new WeightedBlock(blockType, 1.0));
            this.position = Math.max(0.0, Math.min(1.0, position));
            this.totalWeight = 1.0;
        }

        /**
         * Create a stop with multiple equally-weighted block types.
         */
        public GradientStop(List<BlockType> blockTypes, double position) {
            if (blockTypes == null || blockTypes.isEmpty()) {
                throw new IllegalArgumentException("GradientStop must have at least one block type");
            }
            double equalWeight = 1.0 / blockTypes.size();
            this.weightedBlocks = new ArrayList<>();
            for (BlockType bt : blockTypes) {
                this.weightedBlocks.add(new WeightedBlock(bt, equalWeight));
            }
            this.position = Math.max(0.0, Math.min(1.0, position));
            this.totalWeight = 1.0;
        }

        /**
         * Create a stop with weighted block types (weights will be normalized).
         */
        public GradientStop(List<WeightedBlock> weightedBlocks, double position, boolean normalize) {
            if (weightedBlocks == null || weightedBlocks.isEmpty()) {
                throw new IllegalArgumentException("GradientStop must have at least one block type");
            }

            // Calculate total weight for normalization
            double sum = weightedBlocks.stream().mapToDouble(WeightedBlock::getWeight).sum();

            if (normalize && sum > 0) {
                // Normalize weights to sum to 1.0
                this.weightedBlocks = new ArrayList<>();
                for (WeightedBlock wb : weightedBlocks) {
                    this.weightedBlocks.add(new WeightedBlock(wb.getBlockType(), wb.getWeight() / sum));
                }
                this.totalWeight = 1.0;
            } else {
                this.weightedBlocks = new ArrayList<>(weightedBlocks);
                this.totalWeight = sum;
            }

            this.position = Math.max(0.0, Math.min(1.0, position));
        }

        /**
         * Get a block type from this stop using weighted random selection.
         * If multiple blocks are defined, selects based on their weights.
         */
        public BlockType getBlockType(Random random) {
            if (weightedBlocks.size() == 1 || random == null) {
                return weightedBlocks.get(0).getBlockType();
            }

            // Weighted random selection using cumulative probability
            double roll = random.nextDouble() * totalWeight;
            double cumulative = 0.0;

            for (WeightedBlock wb : weightedBlocks) {
                cumulative += wb.getWeight();
                if (roll < cumulative) {
                    return wb.getBlockType();
                }
            }

            // Fallback to last block (shouldn't happen with proper normalization)
            return weightedBlocks.get(weightedBlocks.size() - 1).getBlockType();
        }

        /**
         * Get the primary block type (first in the list).
         */
        public BlockType getBlockType() {
            return weightedBlocks.get(0).getBlockType();
        }

        /**
         * Get all block types for this stop.
         */
        public List<BlockType> getBlockTypes() {
            List<BlockType> types = new ArrayList<>();
            for (WeightedBlock wb : weightedBlocks) {
                types.add(wb.getBlockType());
            }
            return types;
        }

        /**
         * Get all weighted blocks for this stop.
         */
        public List<WeightedBlock> getWeightedBlocks() {
            return new ArrayList<>(weightedBlocks);
        }

        /**
         * Check if this stop has multiple block types.
         */
        public boolean hasMultipleBlocks() {
            return weightedBlocks.size() > 1;
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
        return getBlockAt(position, null);
    }

    /**
     * Get the block at a position with optional randomization for BLENDED mode.
     * @param position Gradient position (0.0-1.0)
     * @param random Random source for BLENDED mode and multi-block stops (can be null for deterministic modes)
     * @return The selected BlockType
     */
    public BlockType getBlockAt(double position, Random random) {
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
            return before.getBlockType(random);
        }
        if (position == after.position) {
            return after.getBlockType(random);
        }

        double range = after.position - before.position;
        double localPosition = (position - before.position) / range;

        return interpolateBlock(before, after, localPosition, random);
    }

    private BlockType interpolateBlock(GradientStop start, GradientStop end, double t, Random random) {
        switch (interpolationMode) {
            case SMOOTH:
                t = t * t * (3 - 2 * t);
                break;
            case DISCRETE:
                return t < 0.5 ? start.getBlockType(random) : end.getBlockType(random);
            case BLENDED:
                if (random != null) {
                    // Probabilistic selection: t determines probability of 'end'
                    // At t=0.0: 0% chance of end (100% start)
                    // At t=0.5: 50% chance of either
                    // At t=1.0: 100% chance of end
                    GradientStop chosen = random.nextDouble() < t ? end : start;
                    return chosen.getBlockType(random);
                }
                // Fallback to linear if no random provided
                break;
            case LINEAR:
            default:
                break;
        }

        return t < 0.5 ? start.getBlockType(random) : end.getBlockType(random);
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

    // Regex pattern for parsing percentage: "50%stone" -> group(1)="50", group(2)="stone"
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)%(.+)$");

    /**
     * Parse a gradient string into a GradientDefinition.
     * Supports multiple blocks per stop using | separator.
     * Supports percentage weights: "50%stone,30%andesite,20%deepslate"
     * Percentages are auto-normalized if they don't sum to 100%.
     *
     * Examples:
     * - "stone,andesite,deepslate" - equal weights for each stop
     * - "stone|cobblestone,andesite,deepslate" - first stop randomly picks stone or cobblestone
     * - "50%stone,30%andesite,20%deepslate" - weighted stops
     * - "50%stone|cobblestone,50%andesite" - weighted stops with random selection within stop
     */
    public static GradientDefinition parse(String gradientString, GradientDirection direction, InterpolationMode mode) {
        String[] stopStrings = gradientString.split(",");
        List<GradientStop> stops = new ArrayList<>();

        for (int i = 0; i < stopStrings.length; i++) {
            String stopString = stopStrings[i].trim();

            // Check if this stop has a percentage prefix (applies to whole stop)
            Matcher percentMatcher = PERCENTAGE_PATTERN.matcher(stopString);
            double stopWeight = -1; // -1 means no explicit weight

            if (percentMatcher.matches()) {
                stopWeight = Double.parseDouble(percentMatcher.group(1));
                stopString = percentMatcher.group(2);
            }

            // Check if this stop has multiple blocks (separated by |)
            String[] blockNames = stopString.split("\\|");
            List<WeightedBlock> weightedBlocks = new ArrayList<>();

            for (String blockName : blockNames) {
                blockName = blockName.trim();

                // Check for per-block percentage within the | group
                Matcher blockPercentMatcher = PERCENTAGE_PATTERN.matcher(blockName);
                double blockWeight = 1.0;

                if (blockPercentMatcher.matches()) {
                    blockWeight = Double.parseDouble(blockPercentMatcher.group(1));
                    blockName = blockPercentMatcher.group(2);
                }

                BlockType blockType = BlockTypes.get(blockName.contains(":") ? blockName : "minecraft:" + blockName);

                if (blockType == null) {
                    throw new IllegalArgumentException("Unknown block type: " + blockName);
                }

                weightedBlocks.add(new WeightedBlock(blockType, blockWeight));
            }

            // Calculate gradient position
            double position = stopStrings.length > 1 ? (double) i / (stopStrings.length - 1) : 0.5;

            // Create the stop with normalized weights
            stops.add(new GradientStop(weightedBlocks, position, true));
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
