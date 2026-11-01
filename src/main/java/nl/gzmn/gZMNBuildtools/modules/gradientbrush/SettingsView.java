package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

/** A player's gradient brush settings as plain values. */
public record SettingsView(String palette, int steps, String axis, String target, String dither, double softness,
                           String shapes, int radius) {
}
