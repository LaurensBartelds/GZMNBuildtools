package nl.gzmn.gZMNBuildtools.integration.worldedit.parser;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import nl.gzmn.gZMNBuildtools.integration.worldedit.pattern.Band;
import nl.gzmn.gZMNBuildtools.integration.worldedit.pattern.BandPattern;
import nl.gzmn.gZMNBuildtools.integration.worldedit.pattern.CyclePattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.integer;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.number;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.suggest;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.usage;

/** Builds the pattern parsers GZMNBuildtools adds to WorldEdit (besides #gradient). */
public final class GzmnPatternParsers {

    private static final java.util.regex.Pattern OPTION = java.util.regex.Pattern.compile("([a-z]+)=(-?[0-9.]+)");
    private static final java.util.regex.Pattern BAND = java.util.regex.Pattern.compile("(-?[0-9.]+):(.+)");

    private GzmnPatternParsers() {
    }

    public static List<GzmnRichParser<Pattern>> create(WorldEdit worldEdit) {
        List<GzmnRichParser<Pattern>> parsers = new ArrayList<>();

        parsers.add(bandParser(worldEdit, "#byslope", BandPattern.Metric.SLOPE,
                "#byslope[0:grass_block][35:stone][60:andesite][radius=2]",
                List.of("radius"), "0:", "30:", "45:", "60:", "radius="));
        parsers.add(bandParser(worldEdit, "#bydepth", BandPattern.Metric.DEPTH,
                "#bydepth[0:grass_block][1:dirt][4:stone]",
                List.of(), "0:", "1:", "3:", "4:"));
        parsers.add(bandParser(worldEdit, "#byheight", BandPattern.Metric.HEIGHT,
                "#byheight[60:sand][66:grass_block][120:stone][160:snow_block][jitter=4][seed=1]",
                List.of("jitter", "seed"), "64:", "100:", "jitter=", "seed="));

        parsers.add(new GzmnRichParser<>(worldEdit, "#checker",
                (in, i, ctx) -> i == 0 ? suggest(in, "1", "2", "4", "8")
                        : worldEdit.getPatternFactory().getSuggestions(in, ctx).stream(),
                (args, ctx) -> {
                    if (args.length < 3) {
                        throw usage("#checker[size][pattern1][pattern2]...  e.g. #checker[2][white_concrete][black_concrete]");
                    }
                    int size = integer(args[0], "size", 1, 256);
                    return CyclePattern.checker(size, subPatterns(worldEdit, args, 1, ctx));
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#stripes",
                (in, i, ctx) -> switch (i) {
                    case 0 -> suggest(in, "x", "y", "z", "xz", "-xz");
                    case 1 -> suggest(in, "1", "2", "3", "4");
                    default -> worldEdit.getPatternFactory().getSuggestions(in, ctx).stream();
                },
                (args, ctx) -> {
                    if (args.length < 4) {
                        throw usage("#stripes[x|y|z|xz|-xz][width][pattern1][pattern2]...  e.g. #stripes[y][2][stone][andesite]");
                    }
                    String axis = args[0].trim().toLowerCase(Locale.ROOT);
                    if (!List.of("x", "y", "z", "xz", "-xz").contains(axis)) {
                        throw new InputParseException("Unknown stripe axis '" + args[0] + "'. Use x, y, z, xz or -xz");
                    }
                    int width = integer(args[1], "width", 1, 256);
                    return CyclePattern.stripes(axis, width, subPatterns(worldEdit, args, 2, ctx));
                }));

        return parsers;
    }

    private static List<Pattern> subPatterns(WorldEdit worldEdit, String[] args, int from, ParserContext ctx)
            throws InputParseException {
        List<Pattern> patterns = new ArrayList<>();
        for (int i = from; i < args.length; i++) {
            patterns.add(worldEdit.getPatternFactory().parseFromInput(args[i], ctx));
        }
        return patterns;
    }

    private static GzmnRichParser<Pattern> bandParser(WorldEdit worldEdit, String alias, BandPattern.Metric metric,
                                                     String example, List<String> allowedOptions,
                                                     String... suggestions) {
        return new GzmnRichParser<>(worldEdit, alias,
                (in, i, ctx) -> {
                    Matcher band = BAND.matcher(in);
                    if (band.matches()) {
                        String head = band.group(1) + ":";
                        return worldEdit.getPatternFactory().getSuggestions(band.group(2), ctx).stream()
                                .map(s -> head + s);
                    }
                    return suggest(in, suggestions);
                },
                (args, ctx) -> {
                    ParsedBands parsed = parseBands(worldEdit, args, allowedOptions, example, ctx);
                    int radius = (int) Math.round(parsed.option("radius", 2));
                    if (radius < 1 || radius > GzmnMaskParsers.MAX_RADIUS) {
                        throw new InputParseException("radius must be between 1 and " + GzmnMaskParsers.MAX_RADIUS);
                    }
                    return new BandPattern(ctx.requireExtent(), metric, parsed.bands(), radius,
                            parsed.option("jitter", 0), (long) parsed.option("seed", 0));
                });
    }

    record ParsedBands(List<Band> bands, Map<String, Double> options) {
        double option(String name, double fallback) {
            return options.getOrDefault(name, fallback);
        }
    }

    static ParsedBands parseBands(WorldEdit worldEdit, String[] args, List<String> allowedOptions, String example,
                                  ParserContext ctx) throws InputParseException {
        List<Band> bands = new ArrayList<>();
        Map<String, Double> options = new HashMap<>();
        for (String raw : args) {
            String arg = raw.trim();
            Matcher option = OPTION.matcher(arg.toLowerCase(Locale.ROOT));
            if (option.matches()) {
                if (!allowedOptions.contains(option.group(1))) {
                    throw new InputParseException("Unknown option '" + option.group(1) + "'"
                            + (allowedOptions.isEmpty() ? "" : ". Allowed: " + String.join(", ", allowedOptions)));
                }
                options.put(option.group(1), number(option.group(2), option.group(1)));
                continue;
            }
            Matcher band = BAND.matcher(arg);
            if (!band.matches()) {
                throw new InputParseException("Expected <threshold>:<pattern> but got '" + raw + "'. Example: " + example);
            }
            Pattern pattern = worldEdit.getPatternFactory().parseFromInput(band.group(2), ctx);
            bands.add(new Band(number(band.group(1), "threshold"), pattern));
        }
        if (bands.isEmpty()) {
            throw usage(example);
        }
        return new ParsedBands(bands, options);
    }

}
