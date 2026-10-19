package nl.gzmn.gZMNBuildtools.brush;

/** A numeric brush parameter with its default and allowed range. */
public record BrushParam(String name, double defaultValue, double min, double max, String description) {
}
