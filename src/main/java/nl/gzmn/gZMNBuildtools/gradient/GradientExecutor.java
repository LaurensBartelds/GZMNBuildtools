package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;

/**
 * Centralized executor for applying gradients to WorldEdit selections.
 * Handles all gradient types through the GradientType interface.
 * Provides WorldEdit integration with undo support.
 */
public class GradientExecutor {

    /**
     * Result of a gradient operation.
     */
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

    /**
     * Apply a gradient to a region using the specified gradient type and definition.
     *
     * @param actor The WorldEdit player actor
     * @param region The region to apply the gradient to
     * @param definition The gradient definition (blocks and settings)
     * @param gradientType The type of gradient (linear, noise, etc.)
     * @param context The gradient context with direction and noise settings
     * @return Result containing success status and block count
     */
    public GradientResult apply(Player actor, Region region, GradientDefinition definition,
                                 GradientType gradientType, GradientContext context) {
        if (definition.getStops().size() < 2) {
            return GradientResult.failure("Gradient must have at least 2 stops");
        }

        int count = 0;
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);

        try (EditSession editSession = localSession.createEditSession(actor)) {
            for (BlockVector3 position : region) {
                double gradientPosition = gradientType.calculatePosition(position, region, context);
                BlockType blockType = definition.getBlockAt(gradientPosition);

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

    /**
     * Apply a linear gradient (convenience method).
     */
    public GradientResult applyLinear(Player actor, Region region, GradientDefinition definition) {
        GradientType linear = new LinearGradient();
        GradientContext context = GradientContext.linear(definition.getDirection());
        return apply(actor, region, definition, linear, context);
    }

    /**
     * Apply a noise gradient with default settings (convenience method).
     */
    public GradientResult applyNoise(Player actor, Region region, GradientDefinition definition) {
        return applyNoise(actor, region, definition, NoiseSettings.defaults());
    }

    /**
     * Apply a noise gradient with custom settings (convenience method).
     */
    public GradientResult applyNoise(Player actor, Region region, GradientDefinition definition,
                                      NoiseSettings noiseSettings) {
        GradientType noise = new NoiseGradient();
        GradientContext context = GradientContext.withNoise(definition.getDirection(), noiseSettings);
        return apply(actor, region, definition, noise, context);
    }

    /**
     * Apply a gradient from a preset.
     */
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

    /**
     * Singleton instance for convenience.
     */
    private static final GradientExecutor INSTANCE = new GradientExecutor();

    public static GradientExecutor getInstance() {
        return INSTANCE;
    }
}
