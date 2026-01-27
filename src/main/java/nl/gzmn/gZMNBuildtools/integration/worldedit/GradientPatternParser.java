package nl.gzmn.gZMNBuildtools.integration.worldedit;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import nl.gzmn.gZMNBuildtools.config.GradientPresets;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

import java.util.stream.Stream;

public class GradientPatternParser extends RichParser<Pattern> {

    private static final String PRESET_PREFIX = "preset:";

    public GradientPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#gradient");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        switch (index) {
            case 0:

                return Stream.of("up", "down", "x", "z", "radial", "u", "d", "r")
                        .filter(s -> s.startsWith(argumentInput.toLowerCase()));
            case 1:

                return Stream.of("linear", "smooth", "discrete", "blended", "noise", "l", "s", "b", "n")
                        .filter(s -> s.startsWith(argumentInput.toLowerCase()));
            case 2:

                if (argumentInput.toLowerCase().startsWith("preset:")) {
                    String partialPreset = argumentInput.substring(7);
                    return GradientPresets.getAllIds().stream()
                            .filter(id -> id.toLowerCase().startsWith(partialPreset.toLowerCase()))
                            .map(id -> "preset:" + id);
                }

                // Preset suggestions when starting fresh
                if (argumentInput.isEmpty()) {
                    Stream<String> presetSuggestions = GradientPresets.getAllIds().stream()
                            .map(id -> "preset:" + id);
                    Stream<String> blockSuggestions = com.sk89q.worldedit.world.block.BlockType.REGISTRY
                            .values().stream()
                            .map(bt -> bt.toString().replace("minecraft:", ""))
                            .sorted()
                            .limit(50)
                            .map(id -> "[" + id + "]");
                    return Stream.concat(presetSuggestions, blockSuggestions);
                }

                // Bracket-aware block suggestions from WorldEdit registry
                boolean insideBracket = argumentInput.lastIndexOf('[') > argumentInput.lastIndexOf(']');

                final String prefix;
                final String searchTerm;
                final String suffix;

                if (insideBracket) {
                    int lastComma = argumentInput.lastIndexOf(',');
                    int lastOpen = argumentInput.lastIndexOf('[');
                    int splitPos = Math.max(lastComma, lastOpen);
                    prefix = argumentInput.substring(0, splitPos + 1);
                    searchTerm = argumentInput.substring(splitPos + 1).toLowerCase();
                    suffix = "]";
                } else if (argumentInput.endsWith("]")) {
                    prefix = argumentInput + "[";
                    searchTerm = "";
                    suffix = "]";
                } else {
                    prefix = "[";
                    searchTerm = argumentInput.toLowerCase();
                    suffix = "]";
                }

                return com.sk89q.worldedit.world.block.BlockType.REGISTRY
                        .values().stream()
                        .map(bt -> bt.toString().replace("minecraft:", ""))
                        .filter(id -> id.startsWith(searchTerm))
                        .sorted()
                        .limit(50)
                        .map(id -> prefix + id + suffix);
            default:
                return Stream.empty();
        }
    }

    @Override
    protected Pattern parseFromInput(String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length < 3) {
            throw new InputParseException(
                    "Invalid gradient syntax. Use: #gradient[direction][mode][blocks]\n" +
                            "Example: #gradient[up][linear][stone,andesite,deepslate]");
        }

        String directionStr = arguments[0].trim();
        String modeStr = arguments[1].trim();

        // RichParser splits on ][ delimiters, which breaks nested brackets like
        // [[50%stone,30%andesite][white_wool]]. Re-join extra arguments to reconstruct
        // the original blocks string.
        String blocksStr;
        if (arguments.length > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < arguments.length; i++) {
                if (i > 2)
                    sb.append("][");
                sb.append(arguments[i]);
            }
            blocksStr = sb.toString();
        } else {
            blocksStr = arguments[2].trim();
        }

        Region region = getRegionFromContext(context);

        GradientDefinition.GradientDirection direction = parseDirection(directionStr);

        boolean useNoise = false;
        NoiseSettings noiseSettings = null;
        GradientDefinition.InterpolationMode mode;

        if (modeStr.equalsIgnoreCase("noise") || modeStr.equalsIgnoreCase("n")) {
            useNoise = true;
            noiseSettings = NoiseSettings.defaults();
            mode = GradientDefinition.InterpolationMode.LINEAR;
        } else {
            mode = parseMode(modeStr);
        }

        if (blocksStr.toLowerCase().startsWith(PRESET_PREFIX)) {
            return parsePresetGradient(blocksStr, direction, mode, region, useNoise, noiseSettings);
        }

        return parseDirectGradient(blocksStr, direction, mode, region, useNoise, noiseSettings);
    }

    private Pattern parsePresetGradient(String blocksStr, GradientDefinition.GradientDirection direction,
                                        GradientDefinition.InterpolationMode mode, Region region,
                                        boolean useNoise, NoiseSettings noiseSettings) throws InputParseException {
        String presetId = blocksStr.substring(PRESET_PREFIX.length()).trim();

        GradientPreset preset = GradientPresets.get(presetId);
        if (preset == null) {
            throw new InputParseException("Unknown gradient preset: " + presetId +
                    ". Available presets: " + String.join(", ", GradientPresets.getAllIds()));
        }

        return GradientPattern.fromPreset(preset, direction, mode, region, useNoise, noiseSettings);
    }

    private Pattern parseDirectGradient(String blocksString, GradientDefinition.GradientDirection direction,
                                        GradientDefinition.InterpolationMode mode, Region region,
                                        boolean useNoise, NoiseSettings noiseSettings) throws InputParseException {

        GradientDefinition definition;
        try {
            definition = GradientDefinition.parse(blocksString, direction, mode);
        } catch (IllegalArgumentException e) {
            throw new InputParseException("Invalid gradient blocks: " + e.getMessage());
        }

        if (useNoise) {
            return GradientPattern.noise(definition, region, noiseSettings);
        } else {
            return GradientPattern.linear(definition, region);
        }
    }

    private GradientDefinition.GradientDirection parseDirection(String dirStr) throws InputParseException {
        switch (dirStr.toLowerCase()) {
            case "up":
            case "u":
            case "vertical_up":
                return GradientDefinition.GradientDirection.VERTICAL_UP;
            case "down":
            case "d":
            case "vertical_down":
                return GradientDefinition.GradientDirection.VERTICAL_DOWN;
            case "x":
            case "horizontal_x":
                return GradientDefinition.GradientDirection.HORIZONTAL_X;
            case "z":
            case "horizontal_z":
                return GradientDefinition.GradientDirection.HORIZONTAL_Z;
            case "radial":
            case "r":
                return GradientDefinition.GradientDirection.RADIAL;
            default:
                throw new InputParseException("Unknown direction: " + dirStr +
                        ". Use: up, down, x, z, or radial");
        }
    }

    private GradientDefinition.InterpolationMode parseMode(String modeStr) throws InputParseException {
        switch (modeStr.toLowerCase()) {
            case "linear":
            case "l":
                return GradientDefinition.InterpolationMode.LINEAR;
            case "smooth":
            case "s":
                return GradientDefinition.InterpolationMode.SMOOTH;
            case "discrete":
                return GradientDefinition.InterpolationMode.DISCRETE;
            case "blended":
            case "b":
                return GradientDefinition.InterpolationMode.BLENDED;
            default:
                throw new InputParseException("Unknown interpolation mode: " + modeStr +
                        ". Use: linear, smooth, discrete, blended, or noise");
        }
    }

    private Region getRegionFromContext(ParserContext context) throws InputParseException {
        try {
            if (context.getSession() != null && context.getWorld() != null) {
                return context.getSession().getSelection(context.getWorld());
            }
        } catch (Exception e) {

        }
        throw new InputParseException("No WorldEdit selection found. Make a selection with //wand first.");
    }
}
