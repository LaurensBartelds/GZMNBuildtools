package nl.gzmn.gZMNBuildtools.modules.weathering.engine;

import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;

import java.util.Locale;

/**
 * How hard to weather a build. {@code strength} scales everything; the other
 * amounts (0 = off, 1 = normal, 2 = double) tune each effect.
 */
public record WeatherOptions(double strength, double moss, double cracks, double decay, double vines, int radius,
                             TargetMode target) {

    public static final String USAGE =
            "[strength=0.5] [moss=1] [cracks=1] [decay=1] [vines=1] [radius=5] [target=build|material]";

    public static WeatherOptions defaults() {
        return new WeatherOptions(0.5, 1, 1, 1, 1, 5, TargetMode.STRUCTURAL);
    }

    /**
     * Parses {@code key=value} options over the defaults.
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public static WeatherOptions parse(String input, int maxRadius) {
        double strength = 0.5;
        double moss = 1;
        double cracks = 1;
        double decay = 1;
        double vines = 1;
        int radius = 5;
        TargetMode target = TargetMode.STRUCTURAL;
        for (String token : input == null ? new String[0] : input.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("Expected key=value but got '" + token + "'. Options: " + USAGE);
            }
            String key = token.substring(0, eq).toLowerCase(Locale.ROOT);
            String value = token.substring(eq + 1).toLowerCase(Locale.ROOT);
            switch (key) {
                case "strength" -> strength = number(value, key, 0, 1);
                case "moss" -> moss = number(value, key, 0, 2);
                case "cracks" -> cracks = number(value, key, 0, 2);
                case "decay" -> decay = number(value, key, 0, 2);
                case "vines" -> vines = number(value, key, 0, 2);
                case "radius", "size" -> radius = (int) number(value, key, 1, maxRadius);
                case "target" -> target = switch (value) {
                    case "build", "structural" -> TargetMode.STRUCTURAL;
                    case "material", "clicked" -> TargetMode.CLICKED;
                    default -> throw new IllegalArgumentException("target must be build or material");
                };
                default -> throw new IllegalArgumentException("Unknown option '" + key + "'. Options: " + USAGE);
            }
        }
        return new WeatherOptions(strength, moss, cracks, decay, vines, radius, target);
    }

    private static double number(String value, String name, double min, double max) {
        double n;
        try {
            n = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
        if (n < min || n > max) {
            throw new IllegalArgumentException(name + " must be between " + fmt(min) + " and " + fmt(max));
        }
        return n;
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    public String describe() {
        return "strength " + fmt(strength) + ", moss " + fmt(moss) + ", cracks " + fmt(cracks) + ", decay "
                + fmt(decay) + ", vines " + fmt(vines) + ", radius " + radius
                + (target == TargetMode.CLICKED ? ", clicked material only" : "");
    }
}
