package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything that configures a player's smart gradient. Immutable; edits
 * return a copy, so a bound brush always sees a consistent set.
 */
public record GradientSettings(String palette, int steps, GradientAxis axis, TargetMode target, Dither dither,
                               double softness, boolean keepShapes, double[] range, int radius) {

    public static final List<String> OPTIONS =
            List.of("steps", "axis", "target", "dither", "softness", "shapes", "range", "radius");

    public static GradientSettings defaults() {
        return new GradientSettings("auto", 0, GradientAxis.AUTO, TargetMode.CLICKED, Dither.ORDERED, 0.6,
                true, null, 4);
    }

    public GradientSettings withPalette(String spec) {
        return new GradientSettings(spec, steps, axis, target, dither, softness, keepShapes, range, radius);
    }

    public GradientSettings withRadius(int r) {
        return new GradientSettings(palette, steps, axis, target, dither, softness, keepShapes, range, r);
    }

    public GradientSettings withAxis(GradientAxis a) {
        return new GradientSettings(palette, steps, a, target, dither, softness, keepShapes, range, radius);
    }

    public GradientSettings withTarget(TargetMode t) {
        return new GradientSettings(palette, steps, axis, t, dither, softness, keepShapes, range, radius);
    }

    public GradientSettings withDither(Dither d) {
        return new GradientSettings(palette, steps, axis, target, d, softness, keepShapes, range, radius);
    }

    public GradientSettings withSoftness(double s) {
        return new GradientSettings(palette, steps, axis, target, dither, Math.max(0, Math.min(1, s)),
                keepShapes, range, radius);
    }

    public GradientSettings withSteps(int s) {
        return new GradientSettings(palette, Math.max(0, s), axis, target, dither, softness, keepShapes, range,
                radius);
    }

    public GradientSettings withKeepShapes(boolean keep) {
        return new GradientSettings(palette, steps, axis, target, dither, softness, keep, range, radius);
    }

    public GradientSettings withRange(double[] r) {
        return new GradientSettings(palette, steps, axis, target, dither, softness, keepShapes, r, radius);
    }

    /**
     * Applies one {@code key=value} option.
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public GradientSettings with(String key, String value, int maxRadius) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "steps" -> withSteps(parseInt(v, "steps", 0, 32));
            case "radius", "size" -> withRadius(parseInt(v, "radius", 1, maxRadius));
            case "axis", "direction", "dir" -> withAxis(GradientAxis.parse(v).orElseThrow(() ->
                    new IllegalArgumentException("axis must be auto, up, down, x, -x, z, -z, radial or inward")));
            case "target", "paint" -> withTarget(TargetMode.parse(v).orElseThrow(() ->
                    new IllegalArgumentException("target must be clicked, main, structural or all")));
            case "dither", "blend" -> withDither(Dither.parse(v).orElseThrow(() ->
                    new IllegalArgumentException("dither must be ordered, noise or none")));
            case "softness", "soft" -> withSoftness(parseDouble(v, "softness", 0, 1));
            case "shapes", "shape" -> switch (v) {
                case "keep", "true", "yes" -> withKeepShapes(true);
                case "full", "false", "no" -> withKeepShapes(false);
                default -> throw new IllegalArgumentException("shapes must be keep or full");
            };
            case "range" -> withRange(parseRange(v));
            default -> throw new IllegalArgumentException("Unknown option '" + key + "'. Options: "
                    + String.join(", ", OPTIONS));
        };
    }

    private static double[] parseRange(String v) {
        if (v.equals("auto") || v.equals("build")) {
            return null;
        }
        int dots = v.indexOf("..");
        if (dots <= 0) {
            throw new IllegalArgumentException("range must look like 60..120 (or auto)");
        }
        double lo = parseDouble(v.substring(0, dots), "range", -4096, 4096);
        double hi = parseDouble(v.substring(dots + 2), "range", -4096, 4096);
        if (lo == hi) {
            throw new IllegalArgumentException("range needs two different values");
        }
        return new double[]{Math.min(lo, hi), Math.max(lo, hi)};
    }

    private static int parseInt(String v, String name, int min, int max) {
        try {
            int n = Integer.parseInt(v);
            if (n < min || n > max) {
                throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    private static double parseDouble(String v, String name, double min, double max) {
        try {
            double n = Double.parseDouble(v);
            if (n < min || n > max) {
                throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
    }

    /** Options in display order, as shown to players. */
    public Map<String, String> describe() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("palette", palette);
        out.put("steps", steps == 0 ? "auto" : String.valueOf(steps));
        out.put("axis", axis.id());
        out.put("target", target.id());
        out.put("dither", dither.name().toLowerCase(Locale.ROOT));
        out.put("softness", String.valueOf(Math.round(softness * 100) / 100.0));
        out.put("shapes", keepShapes ? "keep" : "full");
        out.put("range", range == null ? "build" : fmt(range[0]) + ".." + fmt(range[1]));
        out.put("radius", String.valueOf(radius));
        return out;
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
