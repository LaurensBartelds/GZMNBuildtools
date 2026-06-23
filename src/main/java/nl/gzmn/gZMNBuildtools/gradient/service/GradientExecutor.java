package nl.gzmn.gZMNBuildtools.gradient.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.gradient.strategy.LinearGradient;
import nl.gzmn.gZMNBuildtools.gradient.strategy.NoiseGradient;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

import java.util.Random;


public class GradientExecutor {


    public static class GradientResult {
        private final int blocksAffected;
        private final boolean success;
        private final String errorMessage;

        private GradientResult(int blocksAffected, boolean success, String errorMessage) {
            this.blocksAffected = blocksAffected;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static GradientResult success(int blocksAffected) {
            return new GradientResult(blocksAffected, true, null);
        }

        public static GradientResult failure(String errorMessage) {
            return new GradientResult(0, false, errorMessage);
        }

        public int getBlocksAffected() {
            return blocksAffected;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }


    public GradientResult apply(Player actor, Region region, GradientDefinition definition,
                                GradientType gradientType, GradientContext context) {
        if (definition.getStops().size() < 2) {
            return GradientResult.failure("Gradient must have at least 2 stops");
        }

        int count = 0;
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);


        boolean useBlended = definition.getInterpolationMode() == GradientDefinition.InterpolationMode.BLENDED;
        long seed = context.hasNoise() ? context.getNoiseSettings().getSeed() : System.currentTimeMillis();

        try (EditSession editSession = localSession.createEditSession(actor)) {
            for (BlockVector3 position : region) {
                double gradientPosition = gradientType.calculatePosition(position, region, context);

                BlockType blockType;
                if (useBlended) {

                    Random random = new Random(positionHash(position, seed));
                    blockType = definition.getBlockAt(gradientPosition, random);
                } else {
                    blockType = definition.getBlockAt(gradientPosition);
                }

                if (blockType != null) {
                    editSession.setBlock(position, blockType.getDefaultState());
                    count++;
                }
            }

            localSession.remember(editSession);
        } catch (Exception e) {
            return GradientResult.failure("Error applying gradient: " + e.getMessage());
        }

        return GradientResult.success(count);
    }


    private long positionHash(BlockVector3 position, long seed) {
        return seed ^ (position.x() * 73856093L) ^ (position.y() * 19349663L) ^ (position.z() * 83492791L);
    }


    public GradientResult applyLinear(Player actor, Region region, GradientDefinition definition) {
        GradientType linear = new LinearGradient();
        GradientContext context = GradientContext.linear(definition.getDirection());
        return apply(actor, region, definition, linear, context);
    }


    public GradientResult applyNoise(Player actor, Region region, GradientDefinition definition) {
        return applyNoise(actor, region, definition, NoiseSettings.defaults());
    }


    public GradientResult applyNoise(Player actor, Region region, GradientDefinition definition,
                                     NoiseSettings noiseSettings) {
        GradientType noise = new NoiseGradient();
        GradientContext context = GradientContext.withNoise(definition.getDirection(), noiseSettings);
        return apply(actor, region, definition, noise, context);
    }


    public GradientResult applyPreset(Player actor, Region region, GradientPreset preset,
                                      GradientDefinition.GradientDirection direction,
                                      boolean useNoise, NoiseSettings noiseSettings) {
        GradientDefinition definition = GradientDefinition.fromPreset(preset, direction);

        if (useNoise && noiseSettings != null) {
            return applyNoise(actor, region, definition, noiseSettings);
        } else {
            return applyLinear(actor, region, definition);
        }
    }
}
