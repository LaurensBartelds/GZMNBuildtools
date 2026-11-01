package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Locale;
import java.util.Optional;

/** Which blocks of a build the gradient repaints. */
public enum TargetMode {
    /** Only the material you clicked (plus its stairs/slabs/walls and mossy/cracked forms). */
    CLICKED,
    /** The build's main materials, leaving accent materials alone. */
    MAIN,
    /** Every building block of the build (details such as glass and lights stay). */
    STRUCTURAL,
    /** Every block of the build, details included. */
    ALL;

    public static Optional<TargetMode> parse(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "clicked", "same", "material", "c" -> Optional.of(CLICKED);
            case "main", "m" -> Optional.of(MAIN);
            case "structural", "structure", "build", "s" -> Optional.of(STRUCTURAL);
            case "all", "everything" -> Optional.of(ALL);
            default -> Optional.empty();
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
