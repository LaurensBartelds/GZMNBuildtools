package nl.gzmn.gZMNBuildtools.gradient;

import nl.gzmn.gZMNBuildtools.noise.NoiseGenerator;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;
import nl.gzmn.gZMNBuildtools.noise.PerlinNoise;
import nl.gzmn.gZMNBuildtools.noise.SimplexNoise;

/**
 * Immutable context object passed during gradient calculation.
 * Contains all parameters needed for gradient and noise calculations.
 * Uses builder pattern for safe construction.
 */
public class GradientContext {

    private final GradientDefinition.GradientDirection direction;
    private final NoiseGenerator noiseGenerator;
    private final NoiseSettings noiseSettings;

    private GradientContext(Builder builder) {
        this.direction = builder.direction;
        this.noiseSettings = builder.noiseSettings;

        if (builder.noiseGenerator != null) {
            this.noiseGenerator = builder.noiseGenerator;
        } else if (builder.noiseSettings != null) {
            this.noiseGenerator = createNoiseGenerator(builder.noiseSettings);
        } else {
            this.noiseGenerator = null;
        }
    }

    private static NoiseGenerator createNoiseGenerator(NoiseSettings settings) {
        NoiseGenerator generator;
        switch (settings.getAlgorithm()) {
            case PERLIN:
                generator = new PerlinNoise(settings.getSeed());
                break;
            case SIMPLEX:
            default:
                generator = new SimplexNoise(settings.getSeed());
                break;
        }
        return generator;
    }

    /**
     * Get the gradient direction.
     */
    public GradientDefinition.GradientDirection getDirection() {
        return direction;
    }

    /**
     * Get the noise generator (may be null for linear gradients).
     */
    public NoiseGenerator getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * Get the noise settings (may be null for linear gradients).
     */
    public NoiseSettings getNoiseSettings() {
        return noiseSettings;
    }

    /**
     * Check if noise is enabled for this context.
     */
    public boolean hasNoise() {
        return noiseGenerator != null && noiseSettings != null;
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a simple context for linear gradients without noise.
     */
    public static GradientContext linear(GradientDefinition.GradientDirection direction) {
        return new Builder()
                .direction(direction)
                .build();
    }

    /**
     * Create a context with noise using default settings.
     */
    public static GradientContext withNoise(GradientDefinition.GradientDirection direction) {
        return new Builder()
                .direction(direction)
                .noiseSettings(NoiseSettings.defaults())
                .build();
    }

    /**
     * Create a context with custom noise settings.
     */
    public static GradientContext withNoise(GradientDefinition.GradientDirection direction, NoiseSettings settings) {
        return new Builder()
                .direction(direction)
                .noiseSettings(settings)
                .build();
    }

    /**
     * Builder for GradientContext.
     */
    public static class Builder {
        private GradientDefinition.GradientDirection direction = GradientDefinition.GradientDirection.VERTICAL_UP;
        private NoiseGenerator noiseGenerator;
        private NoiseSettings noiseSettings;

        public Builder direction(GradientDefinition.GradientDirection direction) {
            this.direction = direction;
            return this;
        }

        public Builder noiseGenerator(NoiseGenerator noiseGenerator) {
            this.noiseGenerator = noiseGenerator;
            return this;
        }

        public Builder noiseSettings(NoiseSettings noiseSettings) {
            this.noiseSettings = noiseSettings;
            return this;
        }

        public GradientContext build() {
            return new GradientContext(this);
        }
    }

    @Override
    public String toString() {
        return String.format("GradientContext{direction=%s, hasNoise=%b}", direction, hasNoise());
    }
}
