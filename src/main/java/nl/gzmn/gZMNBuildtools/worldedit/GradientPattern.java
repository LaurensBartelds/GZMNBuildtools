package nl.gzmn.gZMNBuildtools.worldedit;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.gradient.*;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

import java.util.Random;

/**
 * WorldEdit Pattern implementation for gradients.
 * Enables usage with standard WorldEdit commands like //set, //replace, etc.
 *
 * Usage examples (FAWE-style bracket syntax):
 * - //set #gradient[up][linear][stone,andesite,deepslate]
 * - //replace grass #gradient[down][smooth][dirt,stone]
 * - //set #gradient[up][linear][preset:warm_sunset]
 * - //set #gradient[down][blended][50%stone,30%andesite,20%deepslate]
 */
public class GradientPattern implements Pattern {

    private final GradientDefinition definition;
    private final GradientType gradientType;
    private final GradientContext context;
    private final Region region;
    private final long seed;

    /**
     * Create a gradient pattern with region context.
     *
     * @param definition The gradient definition with stops and interpolation mode
     * @param gradientType The gradient type (linear or noise)
     * @param context The gradient context with direction and noise settings
     * @param region The region bounds for position normalization
     */
    public GradientPattern(GradientDefinition definition, GradientType gradientType,
                           GradientContext context, Region region) {
        this.definition = definition;
        this.gradientType = gradientType;
        this.context = context;
        this.region = region;
        this.seed = context.hasNoise() ? context.getNoiseSettings().getSeed() : System.currentTimeMillis();
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        // Calculate gradient position (0.0 - 1.0) based on block location
        double gradientPosition = gradientType.calculatePosition(position, region, context);

        BlockType blockType;
        if (definition.getInterpolationMode() == GradientDefinition.InterpolationMode.BLENDED) {
            // Position-based seed for deterministic per-block results
            Random random = new Random(positionHash(position, seed));
            blockType = definition.getBlockAt(gradientPosition, random);
        } else {
            blockType = definition.getBlockAt(gradientPosition);
        }

        if (blockType != null) {
            return blockType.getDefaultState().toBaseBlock();
        }
        return null;
    }

    /**
     * Create a deterministic hash from block position and seed.
     * Ensures the same position always gets the same random result.
     */
    private long positionHash(BlockVector3 position, long seed) {
        return seed ^ (position.x() * 73856093L) ^ (position.y() * 19349663L) ^ (position.z() * 83492791L);
    }

    /**
     * Get the gradient definition.
     */
    public GradientDefinition getDefinition() {
        return definition;
    }

    /**
     * Get the gradient type.
     */
    public GradientType getGradientType() {
        return gradientType;
    }

    /**
     * Get the gradient context.
     */
    public GradientContext getContext() {
        return context;
    }

    /**
     * Get the region bounds.
     */
    public Region getRegion() {
        return region;
    }

    // ===== Factory Methods =====

    /**
     * Create a linear gradient pattern.
     */
    public static GradientPattern linear(GradientDefinition definition, Region region) {
        GradientType linear = new LinearGradient();
        GradientContext context = GradientContext.linear(definition.getDirection());
        return new GradientPattern(definition, linear, context, region);
    }

    /**
     * Create a noise gradient pattern with default settings.
     */
    public static GradientPattern noise(GradientDefinition definition, Region region) {
        return noise(definition, region, NoiseSettings.defaults());
    }

    /**
     * Create a noise gradient pattern with custom settings.
     */
    public static GradientPattern noise(GradientDefinition definition, Region region, NoiseSettings settings) {
        GradientType noise = new NoiseGradient();
        GradientContext context = GradientContext.withNoise(definition.getDirection(), settings);
        return new GradientPattern(definition, noise, context, region);
    }

    /**
     * Create a gradient pattern from a preset.
     */
    public static GradientPattern fromPreset(GradientPreset preset,
                                              GradientDefinition.GradientDirection direction,
                                              GradientDefinition.InterpolationMode mode,
                                              Region region,
                                              boolean useNoise,
                                              NoiseSettings noiseSettings) {
        GradientDefinition definition = GradientDefinition.fromPreset(preset, direction, mode);

        if (useNoise && noiseSettings != null) {
            return noise(definition, region, noiseSettings);
        } else {
            return linear(definition, region);
        }
    }
}
