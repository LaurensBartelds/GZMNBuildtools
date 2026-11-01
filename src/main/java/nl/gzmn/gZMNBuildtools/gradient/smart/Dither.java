package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Locale;
import java.util.Optional;

/**
 * Per-block thresholds that decide where one palette stop turns into the next.
 * All modes are deterministic per position, so separate brush strokes line up.
 */
public enum Dither {
    /** Even, grain-free spread (a 3D low-discrepancy sequence). */
    ORDERED,
    /** Random-looking speckle. */
    NOISE,
    /** No mixing: clean bands. */
    NONE;

    // R3 sequence constants (inverse powers of the plastic number's 3D analogue).
    private static final double G = 1.2207440846057596;
    private static final double A1 = 1 / G;
    private static final double A2 = 1 / (G * G);
    private static final double A3 = 1 / (G * G * G);

    public double threshold(int x, int y, int z, long seed) {
        return switch (this) {
            case ORDERED -> {
                double v = x * A1 + y * A2 + z * A3 + (seed & 0xFFFF) / 65536.0;
                yield v - Math.floor(v);
            }
            case NOISE -> hash01(x, y, z, seed);
            case NONE -> 0.5;
        };
    }

    /** A random-looking value in [0, 1) for a position. */
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

    public static Optional<Dither> parse(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "ordered", "smooth", "o" -> Optional.of(ORDERED);
            case "noise", "random", "n" -> Optional.of(NOISE);
            case "none", "bands", "off" -> Optional.of(NONE);
            default -> Optional.empty();
        };
    }
}
