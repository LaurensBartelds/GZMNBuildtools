package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Places a tapering spike (spire, stalagmite or stalactite) whose base radius
 * is the brush size.
 */
public class SpikeBrush implements Brush {

    private final int height;
    private final double sharpness;
    private final double tilt;
    private final boolean down;

    public SpikeBrush(int height, double sharpness, double tilt, boolean down) {
        this.height = height;
        this.sharpness = sharpness;
        this.tilt = tilt;
        this.down = down;
    }

    /** Radius of the spike at fraction {@code t} (0 = base, 1 = tip) of its height. */
    public static double radiusAt(double baseRadius, double t, double sharpness) {
        return baseRadius * Math.pow(Math.max(0, 1 - t), sharpness);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        double baseRadius = Math.max(0.5, size);
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double lean = tilt * height;
        double leanX = Math.cos(angle) * lean;
        double leanZ = Math.sin(angle) * lean;
        int direction = down ? -1 : 1;
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();

        for (int level = 0; level < height; level++) {
            int y = position.y() + direction * level;
            if (y < minY || y > maxY) {
                break;
            }
            double t = level / (double) height;
            double r = radiusAt(baseRadius, t, sharpness);
            if (r < 0.35) {
                r = 0.35;
            }
            double cx = position.x() + leanX * t;
            double cz = position.z() + leanZ * t;
            int ir = (int) Math.ceil(r);
            for (int x = (int) Math.floor(cx) - ir; x <= (int) Math.ceil(cx) + ir; x++) {
                for (int z = (int) Math.floor(cz) - ir; z <= (int) Math.ceil(cz) + ir; z++) {
                    double ddx = x - cx;
                    double ddz = z - cz;
                    if (ddx * ddx + ddz * ddz <= r * r + 0.25) {
                        editSession.setBlock(x, y, z, pattern);
                    }
                }
            }
        }
    }
}
