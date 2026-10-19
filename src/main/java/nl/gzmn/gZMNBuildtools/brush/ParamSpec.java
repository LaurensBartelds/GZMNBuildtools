package nl.gzmn.gZMNBuildtools.brush;

import java.util.List;

/** Something configured by an optional pattern plus numeric parameters (brushes, generators). */
public interface ParamSpec {

    String id();

    /** Whether the first argument is a fill pattern. */
    boolean usesPattern();

    List<BrushParam> params();

    String usage();

    static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
