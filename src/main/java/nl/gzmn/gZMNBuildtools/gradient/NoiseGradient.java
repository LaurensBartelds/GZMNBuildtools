package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import nl.gzmn.gZMNBuildtools.noise.NoiseGenerator;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

/**
 * Noise-blended gradient implementation.
 * Combines linear gradient calculation with noise modulation
 * to create natural-looking variation at gradient boundaries.
 */
public class NoiseGradient implements GradientType {

    private static final String ID = "noise";
    private static final String DISPLAY_NAME = "Noise Blend";
    private static final String DESCRIPTION = "Gradient with noise for natural variation at boundaries";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean usesNoise() {
        return true;
    }

    @Override
    public double calculatePosition(BlockVector3 position, Region region, GradientContext context) {
        double basePosition = LinearGradient.calculateLinearPosition(
                position, region, context.getDirection()
        );

        if (!context.hasNoise()) {
            return basePosition;
        }

        NoiseGenerator noise = context.getNoiseGenerator();
        NoiseSettings settings = context.getNoiseSettings();

        double noiseValue = noise.fractalNoise(
                position.x() * settings.getScale(),
                position.y() * settings.getScale(),
                position.z() * settings.getScale(),
                settings.getOctaves(),
                settings.getPersistence(),
                settings.getLacunarity()
        );

        double modulation = noiseValue * settings.getStrength();
        double finalPosition = basePosition + modulation;

        return Math.max(0.0, Math.min(1.0, finalPosition));
    }

    /**
     * Calculate noise-modulated position with explicit noise parameters.
     * Useful for previews or when context is not available.
     *
     * @param position Block position
     * @param region Selection region
     * @param direction Gradient direction
     * @param noise Noise generator to use
     * @param settings Noise settings
     * @return Modulated gradient position (0.0-1.0)
     */
    public static double calculateNoisePosition(BlockVector3 position, Region region,
                                                  GradientDefinition.GradientDirection direction,
                                                  NoiseGenerator noise, NoiseSettings settings) {
        double basePosition = LinearGradient.calculateLinearPosition(position, region, direction);

        double noiseValue = noise.fractalNoise(
                position.x() * settings.getScale(),
                position.y() * settings.getScale(),
                position.z() * settings.getScale(),
                settings.getOctaves(),
                settings.getPersistence(),
                settings.getLacunarity()
        );

        double modulation = noiseValue * settings.getStrength();
        double finalPosition = basePosition + modulation;

        return Math.max(0.0, Math.min(1.0, finalPosition));
    }
}
