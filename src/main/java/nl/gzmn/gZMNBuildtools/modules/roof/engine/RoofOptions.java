package nl.gzmn.gZMNBuildtools.modules.roof.engine;

/** Effective options of one {@code /gzroof} run. */
public record RoofOptions(String style, String axis, int height, int overhang, double noise,
                          boolean stackDown, boolean levelEnds, long seed) {
}
