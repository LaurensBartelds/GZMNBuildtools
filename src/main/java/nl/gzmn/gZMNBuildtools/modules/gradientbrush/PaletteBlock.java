package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

/** A block that automatic palettes may use, with what the web editor shows about it. */
public record PaletteBlock(String id, int rgb, int topRgb, String material, boolean stairs, boolean slab) {
}
