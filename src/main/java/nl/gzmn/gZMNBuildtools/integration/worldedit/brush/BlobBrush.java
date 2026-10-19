package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.noise.PerlinNoise;

import java.util.concurrent.ThreadLocalRandom;

/** Places a sphere whose radius is perturbed by 3D noise, giving organic rocks and bushes. */
public class BlobBrush implements Brush {

    private final double roughness;
    private final double frequency;

    public BlobBrush(double roughness, double frequency) {
        this.roughness = roughness;
        this.frequency = frequency;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        double radius = Math.max(1, size);
        PerlinNoise noise = new PerlinNoise(ThreadLocalRandom.current().nextLong());
        // Sample the noise on the unit sphere direction so lumps scale with the blob.
        double scale = 1.5 * frequency;
        int extent = (int) Math.ceil(radius * (1 + roughness));
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();

        for (int dx = -extent; dx <= extent; dx++) {
            for (int dy = -extent; dy <= extent; dy++) {
                int y = position.y() + dy;
                if (y < minY || y > maxY) {
                    continue;
                }
                for (int dz = -extent; dz <= extent; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > extent) {
                        continue;
                    }
                    double n = distance == 0 ? 0
                            : noise.fractalNoise(dx / distance * scale, dy / distance * scale,
                            dz / distance * scale, 3, 0.5, 2.0);
                    if (distance <= radius * (1 + roughness * Math.max(-1, Math.min(1, n * 1.5)))) {
                        editSession.setBlock(position.x() + dx, y, position.z() + dz, pattern);
                    }
                }
            }
        }
    }
}
