package nl.gzmn.gZMNBuildtools.modules.loft.engine;

/** Effective options of one {@code /gzloft build} run. */
public record LoftOptions(boolean outline, boolean fill, boolean smooth, boolean lowPoly,
                          boolean stackDown, double density, long seed) {
}
