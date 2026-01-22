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

    
    
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)%(.+)$");

    
    public static GradientDefinition parse(String gradientString, GradientDirection direction, InterpolationMode mode) {
        String[] stopStrings = gradientString.split(",");
        List<GradientStop> stops = new ArrayList<>();

        for (int i = 0; i < stopStrings.length; i++) {
            String stopString = stopStrings[i].trim();

            
            Matcher percentMatcher = PERCENTAGE_PATTERN.matcher(stopString);
            double stopWeight = -1; 

            if (percentMatcher.matches()) {
                stopWeight = Double.parseDouble(percentMatcher.group(1));
                stopString = percentMatcher.group(2);
            }

            
            String[] blockNames = stopString.split("\\|");
            List<WeightedBlock> weightedBlocks = new ArrayList<>();

            for (String blockName : blockNames) {
                blockName = blockName.trim();

                
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

            
            double position = stopStrings.length > 1 ? (double) i / (stopStrings.length - 1) : 0.5;

            
            stops.add(new GradientStop(weightedBlocks, position, true));
        }

        return new GradientDefinition(stops, direction, mode);
    }

    
    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction) {
        return parse(preset.getBlocksString(), direction, InterpolationMode.LINEAR);
    }

    
    public static GradientDefinition fromPreset(GradientPreset preset, GradientDirection direction,
            InterpolationMode mode) {
        return parse(preset.getBlocksString(), direction, mode);
    }
}
