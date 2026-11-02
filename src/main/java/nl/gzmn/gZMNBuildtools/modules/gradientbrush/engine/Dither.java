package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.build.PositionHash;

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
        return PositionHash.hash01(x, y, z, seed);
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
