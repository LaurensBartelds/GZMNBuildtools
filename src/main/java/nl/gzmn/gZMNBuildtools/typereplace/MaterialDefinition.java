package nl.gzmn.gZMNBuildtools.typereplace;

/**
 * Describes which connecting variants (stairs, slab, wall, fence, …) a base
 * material has, and the block-id stems used to resolve them. Loaded from
 * {@code block-families.yml}.
 */
public final class MaterialDefinition {

    public final String blockName;
    public final String variantPrefix;
    public final boolean hasStairs;
    public final boolean hasSlab;
    public final boolean hasWall;
    public final boolean hasFence;
    public final boolean hasFenceGate;
    public final boolean hasBars;
    public final boolean hasGrate;
    public final String grateName;
    public final String barsName;

    public MaterialDefinition(String blockName, String variantPrefix, boolean hasStairs, boolean hasSlab,
                              boolean hasWall, boolean hasFence, boolean hasFenceGate,
                              boolean hasBars, boolean hasGrate, String grateName, String barsName) {
        this.blockName = blockName;
        this.variantPrefix = variantPrefix;
        this.hasStairs = hasStairs;
        this.hasSlab = hasSlab;
        this.hasWall = hasWall;
        this.hasFence = hasFence;
        this.hasFenceGate = hasFenceGate;
        this.hasBars = hasBars;
        this.hasGrate = hasGrate;
        this.grateName = grateName;
        this.barsName = barsName;
    }
}
