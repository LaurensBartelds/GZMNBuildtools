package nl.gzmn.gZMNBuildtools.worldedit;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import nl.gzmn.gZMNBuildtools.gradient.*;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parser for the #gradient[...] pattern syntax using FAWE's RichParser.
 * Enables gradients with standard WorldEdit commands.
 *
 * FAWE-style syntax with separate brackets:
 * - #gradient[direction][mode][blocks]
 * - #gradient[direction][mode][preset:name]
 *
 * Examples:
 * - #gradient[up][linear][stone,andesite,deepslate]
 * - #gradient[down][smooth][50%stone,30%andesite,20%deepslate]
 * - #gradient[radial][blended][preset:warm_sunset]
 * - #gradient[x][noise][white_wool,gray_wool,black_wool]
 *
 * Directions: up/u, down/d, x, z, radial/r
 * Modes: linear/l, smooth/s, discrete/d, blended/b, noise/n
 *
 * Blocks support percentages (auto-normalized):
 * - 50%stone,30%andesite,20%deepslate (sums to 100%)
 * - 50%stone,30%andesite (normalized to 62.5%/37.5%)
 * - stone,andesite,deepslate (equal weights)
 */
public class GradientPatternParser extends RichParser<Pattern> {

    private static final String PRESET_PREFIX = "preset:";
    private final Logger logger;

    public GradientPatternParser(WorldEdit worldEdit, Logger logger) {
        super(worldEdit, "#gradient");
        this.logger = logger;
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        switch (index) {
            case 0:
                // Direction suggestions
                return Stream.of("up", "down", "x", "z", "radial", "u", "d", "r")
                        .filter(s -> s.startsWith(argumentInput.toLowerCase()));
            case 1:
                // Mode suggestions
                return Stream.of("linear", "smooth", "discrete", "blended", "noise", "l", "s", "b", "n")
                        .filter(s -> s.startsWith(argumentInput.toLowerCase()));
            case 2:
                // Block suggestions - presets and common blocks
                if (argumentInput.toLowerCase().startsWith("preset:")) {
                    String partialPreset = argumentInput.substring(7);
                    return GradientPresets.getAllIds().stream()
                            .filter(id -> id.toLowerCase().startsWith(partialPreset.toLowerCase()))
                            .map(id -> "preset:" + id);
                }

                // Suggest presets first, then common block combinations
                Stream<String> presets = GradientPresets.getAllIds().stream()
                        .map(id -> "preset:" + id);
                Stream<String> commonBlocks = Stream.of(
                        "stone,andesite,deepslate",
                        "white_wool,gray_wool,black_wool",
                        "grass_block,dirt,stone",
                        "50%stone,30%andesite,20%deepslate");
                return Stream.concat(presets, commonBlocks)
                        .filter(s -> s.toLowerCase().startsWith(argumentInput.toLowerCase()));
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
        String blocksStr = arguments[2].trim();

        logger.info("Parsing gradient: direction=" + directionStr + ", mode=" + modeStr + ", blocks=" + blocksStr);

        // Get the region from context for position normalization
        Region region = getRegionFromContext(context);

        // Parse direction and mode
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

        // Check for preset syntax
        if (blocksStr.toLowerCase().startsWith(PRESET_PREFIX)) {
            return parsePresetGradient(blocksStr, direction, mode, region, useNoise, noiseSettings);
        }

        // Parse direct block specification
        return parseDirectGradient(blocksStr, direction, mode, region, useNoise, noiseSettings);
    }

    /**
     * Parse a preset-based gradient.
     * Syntax: #gradient[direction][mode][preset:name]
     */
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

    /**
     * Parse a direct block gradient with optional percentages.
     * Syntax: #gradient[direction][mode][blocks]
     * Blocks can include percentages: 50%stone,30%andesite,20%deepslate
     */
    private Pattern parseDirectGradient(String blocksString, GradientDefinition.GradientDirection direction,
            GradientDefinition.InterpolationMode mode, Region region,
            boolean useNoise, NoiseSettings noiseSettings) throws InputParseException {
        // Parse the gradient definition (now with percentage support)
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

    /**
     * Parse direction string to enum.
     * Supports full names and shortcuts.
     */
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

    /**
     * Parse mode string to enum.
     * Supports full names and shortcuts.
     */
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

    /**
     * Get the region from parser context.
     * Falls back to a reasonable default if no selection exists.
     */
    private Region getRegionFromContext(ParserContext context) throws InputParseException {
        try {
            if (context.getSession() != null && context.getWorld() != null) {
                return context.getSession().getSelection(context.getWorld());
            }
        } catch (Exception e) {
            // Fall through to error
        }
        throw new InputParseException("No WorldEdit selection found. Make a selection with //wand first.");
    }
}
