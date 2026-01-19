package nl.gzmn.gZMNBuildtools.gradient;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

/**
 * Standard linear gradient implementation.
 * Calculates gradient position purely based on block position and direction.
 * No noise or randomization is applied.
 */
public class LinearGradient implements GradientType {

    private static final String ID = "linear";
    private static final String DISPLAY_NAME = "Linear";
    private static final String DESCRIPTION = "Smooth gradient following the direction without variation";

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
        return false;
    }

    @Override
    public double calculatePosition(BlockVector3 position, Region region, GradientContext context) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        double value;
        double minValue;
        double maxValue;

        switch (context.getDirection()) {
            case VERTICAL_UP:
                minValue = min.y();
                maxValue = max.y();
                value = position.y();
                break;

            case VERTICAL_DOWN:
                minValue = min.y();
                maxValue = max.y();
                value = maxValue - (position.y() - minValue);
                break;

            case HORIZONTAL_X:
                minValue = min.x();
                maxValue = max.x();
                value = position.x();
                break;

            case HORIZONTAL_Z:
                minValue = min.z();
                maxValue = max.z();
                value = position.z();
                break;

            case RADIAL:
                BlockVector3 center = region.getCenter().toBlockPoint();
                minValue = 0;
                maxValue = Math.max(
                        Math.max(Math.abs(max.x() - center.x()), Math.abs(min.x() - center.x())),
                        Math.max(Math.abs(max.z() - center.z()), Math.abs(min.z() - center.z()))
                );
                double dx = position.x() - center.x();
                double dz = position.z() - center.z();
                value = Math.sqrt(dx * dx + dz * dz);
                break;

            default:
                return 0.5;
        }

        double range = maxValue - minValue;
        if (range == 0) {
            return 0.5;
        }

        double normalized = (value - minValue) / range;
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    /**
     * Calculate the base linear position for use by other gradient types.
     * This is a static utility method that can be reused.
     */
    public static double calculateLinearPosition(BlockVector3 position, Region region,
                                                   GradientDefinition.GradientDirection direction) {
        LinearGradient linear = new LinearGradient();
        GradientContext context = GradientContext.linear(direction);
        return linear.calculatePosition(position, region, context);
    }
}
