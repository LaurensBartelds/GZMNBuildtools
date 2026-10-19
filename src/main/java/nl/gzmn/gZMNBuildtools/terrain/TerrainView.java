package nl.gzmn.gZMNBuildtools.terrain;

/**
 * Minimal read-only view of a block world used by the terrain analysis code.
 * Kept free of WorldEdit types so the maths can be unit-tested with a fake world.
 */
public interface TerrainView {

    /** Whether the block at the given position counts as solid terrain. */
    boolean isSolid(int x, int y, int z);

    int minY();

    int maxY();
}
