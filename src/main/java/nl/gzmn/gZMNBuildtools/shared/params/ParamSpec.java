package nl.gzmn.gZMNBuildtools.shared.params;

import java.util.List;

/** Something configured by fill patterns plus parameters (brushes, generators). */
public interface ParamSpec {

    String id();

    /** Whether the first argument is a fill pattern. */
    default boolean usesPattern() {
        return !patternNames().isEmpty();
    }

    /** Names of the leading pattern arguments, e.g. ["log", "leaves"]; empty for none. */
    List<String> patternNames();

    default int patternSlots() {
        return patternNames().size();
    }

    List<BrushParam> params();

    String usage();

    static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
