package nl.gzmn.gZMNBuildtools.noise;

/**
 * Interface for noise generation algorithms.
 * Implementations provide different noise characteristics (Simplex, Perlin, etc.)
 * for use in gradient blending and procedural generation.
 */
public interface NoiseGenerator {

    /**
     * Get the unique identifier for this noise algorithm.
     * @return Algorithm identifier (e.g., "simplex", "perlin")
     */
    String getId();

    /**
     * Get the human-readable display name.
     * @return Display name for UI
     */
    String getDisplayName();

    /**
     * Get noise value at 2D coordinates.
     * @param x X coordinate
     * @param y Y coordinate
     * @return Value in range [-1.0, 1.0]
     */
    double noise(double x, double y);

    /**
     * Get noise value at 3D coordinates.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Value in range [-1.0, 1.0]
     */
    double noise(double x, double y, double z);

    /**
     * Get fractal/octave noise with multiple layers for more natural-looking results.
     * Combines multiple noise samples at different frequencies.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param octaves Number of noise layers to combine (1-8 typical)
     * @param persistence Amplitude multiplier per octave (0.5 typical)
     * @param lacunarity Frequency multiplier per octave (2.0 typical)
     * @return Combined noise value, normalized to approximately [-1.0, 1.0]
     */
    double fractalNoise(double x, double y, double z, int octaves, double persistence, double lacunarity);

    /**
     * Set the seed for reproducible noise patterns.
     * @param seed The seed value
     */
    void setSeed(long seed);

    /**
     * Get the current seed.
     * @return Current seed value
     */
    long getSeed();

    /**
     * Create a new instance with a different seed.
     * @param seed The new seed
     * @return New NoiseGenerator instance with the specified seed
     */
    NoiseGenerator withSeed(long seed);
}
