package nl.gzmn.gZMNBuildtools.shared.params;

import java.util.List;

/**
 * A brush/generator parameter: a number with a range, or (when {@code choices}
 * is non-empty) one of several named options stored as its index.
 */
public record BrushParam(String name, double defaultValue, double min, double max, String description,
                         List<String> choices) {

    public BrushParam(String name, double defaultValue, double min, double max, String description) {
        this(name, defaultValue, min, max, description, List.of());
    }

    public static BrushParam choice(String name, String description, String... choices) {
        return new BrushParam(name, 0, 0, choices.length - 1, description, List.of(choices));
    }

    public boolean isChoice() {
        return !choices.isEmpty();
    }

    /** Display form of a value: the choice name, or the number. */
    public String display(double value) {
        if (isChoice()) {
            int i = (int) Math.round(value);
            return i >= 0 && i < choices.size() ? choices.get(i) : String.valueOf(i);
        }
        return ParamSpec.format(value);
    }
}
