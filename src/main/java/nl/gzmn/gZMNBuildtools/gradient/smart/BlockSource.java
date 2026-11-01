package nl.gzmn.gZMNBuildtools.gradient.smart;

/** Read-only access to block ids (without namespace) for scanning builds. */
public interface BlockSource {

    /** The block id at a position, or null for air. */
    String blockAt(int x, int y, int z);

    int minY();

    int maxY();
}
