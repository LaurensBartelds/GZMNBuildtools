package nl.gzmn.gZMNBuildtools.gradient.model;

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

        public GradientStop(BlockType blockType, double position) {
            this.weightedBlocks = Arrays.asList(new WeightedBlock(blockType, 1.0));
            this.position = Math.max(0.0, Math.min(1.0, position));
            this.totalWeight = 1.0;
        }

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

        public GradientStop(List<WeightedBlock> weightedBlocks, double position, boolean normalize) {
            if (weightedBlocks == null || weightedBlocks.isEmpty()) {
                throw new IllegalArgumentException("GradientStop must have at least one block type");
            }

            double sum = weightedBlocks.stream().mapToDouble(WeightedBlock::getWeight).sum();

            if (normalize && sum > 0) {

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

        public BlockType getBlockType(Random random) {
            if (weightedBlocks.size() == 1 || random == null) {
                return weightedBlocks.get(0).getBlockType();
            }

            double roll = random.nextDouble() * totalWeight;
            double cumulative = 0.0;

            for (WeightedBlock wb : weightedBlocks) {
                cumulative += wb.getWeight();
                if (roll < cumulative) {
                    return wb.getBlockType();
                }
            }

            return weightedBlocks.get(weightedBlocks.size() - 1).getBlockType();
        }

        public BlockType getBlockType() {
            return weightedBlocks.get(0).getBlockType();
        }

        public List<BlockType> getBlockTypes() {
            List<BlockType> types = new ArrayList<>();
            for (WeightedBlock wb : weightedBlocks) {
                types.add(wb.getBlockType());
            }
            return types;
        }

        public List<WeightedBlock> getWeightedBlocks() {
            return new ArrayList<>(weightedBlocks);
        }

        public boolean hasMultipleBlocks() {
            return weightedBlocks.size() > 1;
        }

        public double getPosition() {
            return position;
        }
    }

    public GradientDefinition(List<GradientStop> stops, GradientDirection direction,
                              InterpolationMode interpolationMode) {
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
                        new GradientStop(end, 1.0)),
                direction,
                InterpolationMode.LINEAR);
    }

    public BlockType getBlockAt(double position) {
        return getBlockAt(position, null);
    }

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

                    GradientStop chosen = random.nextDouble() < t ? end : start;
                    return chosen.getBlockType(random);
                }

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

    // Pattern for parsing percentage prefixes like "50%stone"
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)%(.+)$");

    /**
     * Parse a gradient string. Supports two formats:
     * - Bracket format: [stone][dirt][andesite] or [50%stone|50%mossy_stone][dirt]
     * - Legacy comma format: stone,dirt,andesite
     */
    public static GradientDefinition parse(String gradientString, GradientDirection direction, InterpolationMode mode) {
        // Detect format: if starts with [ use bracket format
        if (gradientString.startsWith("[")) {
            return parseBracketFormat(gradientString, direction, mode);
        }
        return parseCommaFormat(gradientString, direction, mode);
    }

    /**
     * Parse bracket format. Supports:
     * - Single block layer: [stone]
     * - Multi-block layer: [[stone][cobblestone,stone_bricks]]
     * Example: [[stone][cobblestone,stone_bricks]][dirt] = 2 layers
     */
    private static GradientDefinition parseBracketFormat(String gradientString, GradientDirection direction,
                                                         InterpolationMode mode) {
        List<GradientStop> stops = new ArrayList<>();
        List<String> layers = new ArrayList<>();

        // Strip optional outer wrapper brackets: [[layer1][layer2]] -> [layer1][layer2]
        if (gradientString.startsWith("[[")) {
            gradientString = gradientString.substring(1, gradientString.length() - 1);
        }

        // Parse layers: each [content] is one layer
        int i = 0;
        while (i < gradientString.length()) {
            if (gradientString.charAt(i) == '[') {
                int start = i;
                i++;
                while (i < gradientString.length() && gradientString.charAt(i) != ']') {
                    i++;
                }
                if (i < gradientString.length()) {
                    layers.add(gradientString.substring(start, i + 1));
                    i++;
                }
            } else {
                i++;
            }
        }

        // Parse each layer - commas separate multiple blocks within a layer
        for (int l = 0; l < layers.size(); l++) {
            String layer = layers.get(l);
            List<WeightedBlock> weightedBlocks = new ArrayList<>();

            if (layer.startsWith("[") && layer.endsWith("]")) {
                String blockContent = layer.substring(1, layer.length() - 1);
                // Split by comma for multiple blocks in the same layer
                String[] blocks = blockContent.split(",");
                for (String block : blocks) {
                    block = block.trim();
                    if (!block.isEmpty()) {
                        weightedBlocks.addAll(parseBlocksInLayer(block));
                    }
                }
            }

            if (!weightedBlocks.isEmpty()) {
                double position = layers.size() > 1 ? (double) l / (layers.size() - 1) : 0.5;
                stops.add(new GradientStop(weightedBlocks, position, true));
            }
        }

        if (stops.size() < 2) {
            throw new IllegalArgumentException("Gradient must have at least 2 layers");
        }

        return new GradientDefinition(stops, direction, mode);
    }

    /**
     * Parse legacy comma format: stone,dirt,andesite
     */
    private static GradientDefinition parseCommaFormat(String gradientString, GradientDirection direction,
                                                       InterpolationMode mode) {
        String[] stopStrings = gradientString.split(",");
        List<GradientStop> stops = new ArrayList<>();

        for (int i = 0; i < stopStrings.length; i++) {
            String stopString = stopStrings[i].trim();
            List<WeightedBlock> weightedBlocks = parseBlocksInLayer(stopString);

            double position = stopStrings.length > 1 ? (double) i / (stopStrings.length - 1) : 0.5;
            stops.add(new GradientStop(weightedBlocks, position, true));
        }

        return new GradientDefinition(stops, direction, mode);
    }

    /**
     * Parse blocks within a single layer. Supports:
     * - Single block: stone
     * - Multiple weighted blocks: 50%stone|50%mossy_stone
     * - Percentage prefix: 30%stone
     */
    private static List<WeightedBlock> parseBlocksInLayer(String stopString) {
        // Check for overall stop weight prefix
        Matcher percentMatcher = PERCENTAGE_PATTERN.matcher(stopString);
        if (percentMatcher.matches()) {
            // Has a leading percentage, strip it
            stopString = percentMatcher.group(2);
        }

        // Split by pipe for multiple blocks in same layer
        String[] blockNames = stopString.split("\\|");
        List<WeightedBlock> weightedBlocks = new ArrayList<>();

        for (String blockName : blockNames) {
            blockName = blockName.trim();

            // Check for individual block weight
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

        return weightedBlocks;
    }

    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction) {
        return parse(preset.getBlocksString(), direction, InterpolationMode.LINEAR);
    }

    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction,
                                                InterpolationMode mode) {
        return parse(preset.getBlocksString(), direction, mode);
    }

    public List<String> getBlockIds() {
        List<String> ids = new ArrayList<>();
        for (GradientStop stop : stops) {
            if (stop.hasMultipleBlocks()) {
                for (WeightedBlock wb : stop.getWeightedBlocks()) {
                    ids.add(wb.getBlockType().toString().replace("minecraft:", ""));
                }
            } else {
                ids.add(stop.getBlockType().toString().replace("minecraft:", ""));
            }
        }
        return ids;
    }
}
