package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Locale;
import java.util.Optional;

/** Which way the gradient runs across a build. */
public enum GradientAxis {
    /** Pick from the build's shape: up for walls and towers, along the length for floors and paths. */
    AUTO,
    UP, DOWN, X, NEG_X, Z, NEG_Z,
    /** From the vertical centre line outwards. */
    RADIAL,
    /** From the outside towards the centre line. */
    INWARD;

    public static Optional<GradientAxis> parse(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "auto", "a" -> Optional.of(AUTO);
            case "up", "u", "y", "vertical_up" -> Optional.of(UP);
            case "down", "d", "-y", "vertical_down" -> Optional.of(DOWN);
            case "x", "east", "+x", "horizontal_x" -> Optional.of(X);
            case "-x", "west" -> Optional.of(NEG_X);
            case "z", "south", "+z", "horizontal_z" -> Optional.of(Z);
            case "-z", "north" -> Optional.of(NEG_Z);
            case "radial", "out", "r" -> Optional.of(RADIAL);
            case "inward", "in" -> Optional.of(INWARD);
            default -> Optional.empty();
        };
    }

    public String id() {
        return switch (this) {
            case NEG_X -> "-x";
            case NEG_Z -> "-z";
            default -> name().toLowerCase(Locale.ROOT);
        };
    }
}
