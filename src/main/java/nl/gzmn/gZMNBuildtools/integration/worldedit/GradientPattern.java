package nl.gzmn.gZMNBuildtools.integration.worldedit;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.gradient.strategy.LinearGradient;
import nl.gzmn.gZMNBuildtools.gradient.strategy.NoiseGradient;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

import java.util.Random;


public class GradientPattern implements Pattern {

    private final GradientDefinition definition;
    private final GradientType gradientType;
    private final GradientContext context;
    private final Region region;
    private final long seed;

    
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
        
        double gradientPosition = gradientType.calculatePosition(position, region, context);

        BlockType blockType;
        if (definition.getInterpolationMode() == GradientDefinition.InterpolationMode.BLENDED) {
            
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

    
    private long positionHash(BlockVector3 position, long seed) {
        return seed ^ (position.x() * 73856093L) ^ (position.y() * 19349663L) ^ (position.z() * 83492791L);
    }

    
    public GradientDefinition getDefinition() {
        return definition;
    }

    
    public GradientType getGradientType() {
        return gradientType;
    }

    
    public GradientContext getContext() {
        return context;
    }

    
    public Region getRegion() {
        return region;
    }

    

    
    public static GradientPattern linear(GradientDefinition definition, Region region) {
        GradientType linear = new LinearGradient();
        GradientContext context = GradientContext.linear(definition.getDirection());
        return new GradientPattern(definition, linear, context, region);
    }

    
    public static GradientPattern noise(GradientDefinition definition, Region region) {
        return noise(definition, region, NoiseSettings.defaults());
    }

    
    public static GradientPattern noise(GradientDefinition definition, Region region, NoiseSettings settings) {
        GradientType noise = new NoiseGradient();
        GradientContext context = GradientContext.withNoise(definition.getDirection(), settings);
        return new GradientPattern(definition, noise, context, region);
    }

    
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
