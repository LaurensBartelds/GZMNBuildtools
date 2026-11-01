package nl.gzmn.gZMNBuildtools.shared.build;

/** Deterministic random-looking numbers per block position, so repeated strokes agree. */
public final class PositionHash {

    private PositionHash() {
    }

    /** A value in [0, 1) for a position and seed. */
    public static double hash01(int x, int y, int z, long seed) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= x * 0xBF58476D1CE4E5B9L;
        h = Long.rotateLeft(h, 31) ^ y * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 27) ^ z * 0xD6E8FEB86659FD93L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Seed shared by everything done to the build with these bounds. */
    public static long seedFor(Bounds bounds) {
        return bounds.minX() * 73856093L ^ bounds.minY() * 19349663L ^ bounds.minZ() * 83492791L;
    }
}
