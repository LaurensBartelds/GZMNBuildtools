package nl.gzmn.gZMNBuildtools.integration.worldedit.pattern;

import nl.gzmn.gZMNBuildtools.noise.PerlinNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Smooth 2D noise in [-1, 1] used to make band boundaries look natural. */
final class Jitter {

    private static final double SCALE = 1 / 12.0;
    private static final Map<Long, PerlinNoise> NOISES = new ConcurrentHashMap<>();

    private Jitter() {
    }

    static double value(long seed, int x, int z) {
        PerlinNoise noise = NOISES.computeIfAbsent(seed, PerlinNoise::new);
        return Math.max(-1, Math.min(1, noise.noise(x * SCALE, z * SCALE) * 1.5));
    }
}
