package nl.gzmn.gZMNBuildtools.modules.scatter.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How the scatter brush places prefabs.
 *
 * @param spacing  least distance between prefabs, or 0 to work it out from their size
 * @param density  how full the area gets (0.05 = a few, 1 = packed)
 * @param maxSlope steepest ground (degrees) a prefab is placed on
 * @param rotate   turn each prefab a random quarter turn
 * @param flip     mirror prefabs at random
 * @param anyGround also place on non-natural blocks (roofs, paths, ...)
 * @param replace  overwrite solid blocks instead of only filling air and plants
 * @param sink     blocks to bury prefabs by, or -1 to bury a schematic's own ground layer
 */
public record ScatterOptions(double spacing, double density, double maxSlope, boolean rotate, boolean flip,
                             boolean anyGround, boolean replace, int sink) {

    public static final ScatterOptions DEFAULTS = new ScatterOptions(0, 0.5, 35, true, false, false, false, -1);

    public static final List<String> NAMES = List.of("spacing=", "density=", "slope=", "rotate=", "flip=", "on=",
            "replace=", "sink=");

    /** Parses {@code name=value} tokens over the defaults. */
    public static ScatterOptions parse(List<String> tokens) {
        double spacing = DEFAULTS.spacing;
        double density = DEFAULTS.density;
        double slope = DEFAULTS.maxSlope;
        boolean rotate = DEFAULTS.rotate;
        boolean flip = DEFAULTS.flip;
        boolean any = DEFAULTS.anyGround;
        boolean replace = DEFAULTS.replace;
        int sink = DEFAULTS.sink;
        for (String token : tokens) {
            int eq = token.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("Expected name=value, got '" + token + "'");
            }
            String name = token.substring(0, eq).toLowerCase(Locale.ROOT);
            String value = token.substring(eq + 1).toLowerCase(Locale.ROOT);
            switch (name) {
                case "spacing" -> spacing = number(name, value, 0, 64);
                case "density" -> density = number(name, value, 0.05, 1);
                case "slope" -> slope = number(name, value, 0, 90);
                case "rotate" -> rotate = flag(name, value);
                case "flip" -> flip = flag(name, value);
                case "on" -> any = switch (value) {
                    case "terrain", "ground", "natural" -> false;
                    case "any", "all" -> true;
                    default -> throw new IllegalArgumentException("on must be terrain or any");
                };
                case "replace" -> replace = switch (value) {
                    case "keep", "air", "no", "0" -> false;
                    case "all", "yes", "1" -> true;
                    default -> throw new IllegalArgumentException("replace must be keep or all");
                };
                case "sink" -> sink = value.equals("auto") ? -1 : (int) number(name, value, 0, 8);
                default -> throw new IllegalArgumentException("Unknown option '" + name + "' (use "
                        + String.join(", ", NAMES) + ")");
            }
        }
        return new ScatterOptions(spacing, density, slope, rotate, flip, any, replace, sink);
    }

    private static double number(String name, String value, double min, double max) {
        double v;
        try {
            v = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " needs a number, got '" + value + "'");
        }
        if (v < min || v > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
        return v;
    }

    private static boolean flag(String name, String value) {
        return switch (value) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> throw new IllegalArgumentException(name + " must be 1 or 0");
        };
    }

    public String describe() {
        List<String> parts = new ArrayList<>();
        parts.add(spacing > 0 ? "spacing " + spacing : "auto spacing");
        parts.add("density " + density);
        parts.add("max slope " + (int) maxSlope + "°");
        if (!rotate) {
            parts.add("no rotation");
        }
        if (flip) {
            parts.add("mirroring");
        }
        if (anyGround) {
            parts.add("on any ground");
        }
        if (replace) {
            parts.add("replacing blocks");
        }
        return String.join(", ", parts);
    }
}
