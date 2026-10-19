package nl.gzmn.gZMNBuildtools.integration.worldedit.parser;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.Direction;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.CheckerMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.CurvatureMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.DepthMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.FacingMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.HeightMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.NearMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.SkyMask;
import nl.gzmn.gZMNBuildtools.integration.worldedit.mask.SlopeMask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.integer;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.number;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.suggest;
import static nl.gzmn.gZMNBuildtools.integration.worldedit.parser.ParserArgs.usage;

/** Builds the terrain/utility mask parsers GZMNBuildtools adds to WorldEdit. */
public final class GzmnMaskParsers {

    public static final int MAX_RADIUS = 16;
    private static final double DEFAULT_CURVATURE = 0.3;
    private static final List<String> FACES = List.of("up", "down", "north", "south", "east", "west");

    private GzmnMaskParsers() {
    }

    public static List<GzmnRichParser<Mask>> create(WorldEdit worldEdit) {
        List<GzmnRichParser<Mask>> parsers = new ArrayList<>();

        parsers.add(new GzmnRichParser<>(worldEdit, "#slope",
                (in, i, ctx) -> i < 2 ? suggest(in, "0", "15", "30", "45", "60", "90") : suggest(in, "1", "2", "3"),
                (args, ctx) -> {
                    if (args.length < 2 || args.length > 3) {
                        throw usage("#slope[minDegrees][maxDegrees][radius=2]  e.g. #slope[30][90]");
                    }
                    int radius = args.length == 3 ? integer(args[2], "radius", 1, MAX_RADIUS) : 2;
                    return new SlopeMask(ctx.requireExtent(), number(args[0], "min"), number(args[1], "max"), radius);
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#curvature",
                (in, i, ctx) -> i < 2 ? suggest(in, "-1", "-0.3", "0", "0.3", "1") : suggest(in, "2", "3", "4"),
                (args, ctx) -> {
                    if (args.length < 2 || args.length > 3) {
                        throw usage("#curvature[min][max][radius=3]  (positive = concave, negative = convex)");
                    }
                    int radius = args.length == 3 ? integer(args[2], "radius", 1, MAX_RADIUS) : 3;
                    return new CurvatureMask(ctx.requireExtent(), number(args[0], "min"), number(args[1], "max"),
                            radius);
                }));

        parsers.add(curvatureShortcut(worldEdit, "#concave", true));
        parsers.add(curvatureShortcut(worldEdit, "#convex", false));

        parsers.add(new GzmnRichParser<>(worldEdit, "#depth",
                (in, i, ctx) -> suggest(in, "0", "1", "2", "3", "4", "8"),
                (args, ctx) -> {
                    if (args.length < 1 || args.length > 2) {
                        throw usage("#depth[min][max]  e.g. #depth[1][3] for the 3 blocks below the surface");
                    }
                    int min = integer(args[0], "min", 0, 4096);
                    int max = args.length == 2 ? integer(args[1], "max", min, 4096) : min;
                    return new DepthMask(ctx.requireExtent(), min, max);
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#height",
                (in, i, ctx) -> suggest(in, "-64", "0", "63", "64", "100", "128", "200", "320"),
                (args, ctx) -> {
                    if (args.length != 2) {
                        throw usage("#height[minY][maxY]  e.g. #height[62][80]");
                    }
                    int min = integer(args[0], "minY", -4096, 4096);
                    return new HeightMask(min, integer(args[1], "maxY", min, 4096));
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#sky",
                (in, i, ctx) -> Stream.empty(),
                (args, ctx) -> new SkyMask(ctx.requireExtent())));

        parsers.add(new GzmnRichParser<>(worldEdit, "#facing",
                (in, i, ctx) -> {
                    int comma = in.lastIndexOf(',');
                    String head = in.substring(0, comma + 1);
                    return suggest(in.substring(comma + 1), FACES.toArray(String[]::new)).map(f -> head + f);
                },
                (args, ctx) -> {
                    if (args.length != 1) {
                        throw usage("#facing[up|down|north|south|east|west,...]  e.g. #facing[up]");
                    }
                    List<Direction> faces = new ArrayList<>();
                    for (String raw : args[0].split(",")) {
                        faces.add(parseFace(raw));
                    }
                    return new FacingMask(ctx.requireExtent(), faces);
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#near",
                (in, i, ctx) -> i == 0
                        ? worldEdit.getMaskFactory().getSuggestions(in, ctx).stream()
                        : suggest(in, "1", "2", "3", "4", "6", "8"),
                (args, ctx) -> {
                    if (args.length != 2) {
                        throw usage("#near[mask][radius]  e.g. #near[water][4]");
                    }
                    Mask inner = worldEdit.getMaskFactory().parseFromInput(args[0], ctx);
                    return new NearMask(inner, integer(args[1], "radius", 1, MAX_RADIUS));
                }));

        parsers.add(new GzmnRichParser<>(worldEdit, "#checker",
                (in, i, ctx) -> suggest(in, "1", "2", "3", "4", "8"),
                (args, ctx) -> new CheckerMask(args.length == 0 ? 1 : integer(args[0], "size", 1, 256))));

        return parsers;
    }

    private static GzmnRichParser<Mask> curvatureShortcut(WorldEdit worldEdit, String alias, boolean concave) {
        return new GzmnRichParser<>(worldEdit, alias,
                (in, i, ctx) -> i == 0 ? suggest(in, "0.1", "0.3", "0.5", "1") : suggest(in, "2", "3", "4"),
                (args, ctx) -> {
                    if (args.length > 2) {
                        throw usage(alias + "[threshold=0.3][radius=3]");
                    }
                    double threshold = args.length >= 1 ? Math.abs(number(args[0], "threshold")) : DEFAULT_CURVATURE;
                    int radius = args.length == 2 ? integer(args[1], "radius", 1, MAX_RADIUS) : 3;
                    return concave
                            ? new CurvatureMask(ctx.requireExtent(), threshold, Double.MAX_VALUE, radius)
                            : new CurvatureMask(ctx.requireExtent(), -Double.MAX_VALUE, -threshold, radius);
                });
    }

    static Direction parseFace(String raw) throws InputParseException {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "up", "u", "top" -> Direction.UP;
            case "down", "d", "bottom" -> Direction.DOWN;
            case "north", "n" -> Direction.NORTH;
            case "south", "s" -> Direction.SOUTH;
            case "east", "e" -> Direction.EAST;
            case "west", "w" -> Direction.WEST;
            default -> throw new InputParseException("Unknown face '" + raw + "'. Use: " + String.join(", ", FACES));
        };
    }
}
