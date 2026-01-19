package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

/**
 * Strategy interface for different gradient calculation types.
 * Implementations determine how block positions map to gradient positions,
 * allowing for different gradient behaviors (linear, noise-blended, etc.)
 */
public interface GradientType {

    /**
     * Get the unique identifier for this gradient type.
     * @return Identifier string (e.g., "linear", "noise")
     */
    String getId();

    /**
     * Get the human-readable display name for UI.
     * @return Display name
     */
    String getDisplayName();

    /**
     * Calculate the gradient position (0.0-1.0) for a given block position.
     * This is the core method that determines which block from the gradient
     * should be placed at a given position.
     *
     * @param position The block position to calculate for
     * @param region The full region being processed
     * @param context Additional context (direction, noise settings, etc.)
     * @return A value between 0.0 and 1.0 representing gradient position
     */
    double calculatePosition(BlockVector3 position, Region region, GradientContext context);

    /**
     * Whether this gradient type uses noise/randomization.
     * Useful for UI to show/hide noise settings.
     * @return true if noise is used
     */
    boolean usesNoise();

    /**
     * Get a brief description of this gradient type for tooltips.
     * @return Description string
     */
    String getDescription();
}
