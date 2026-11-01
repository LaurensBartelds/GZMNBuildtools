package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;
import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Places objects (trees, rocks) on the ground: one at the clicked block, or
 * {@code count} scattered over the surface within the brush radius.
 */
public abstract class ScatterBrush implements Brush {

    private final int count;

    protected ScatterBrush(int count) {
        this.count = Math.max(1, count);
    }

    /** Places one object whose base sits on the ground block (x, groundY, z). */
    protected abstract void place(EditSession editSession, int x, int groundY, int z, Pattern pattern, double size,
                                  Random random);

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        Random random = new Random(ThreadLocalRandom.current().nextLong());
        if (count == 1) {
            place(editSession, position.x(), position.y(), position.z(), pattern, size, random);
            return;
        }
        ExtentTerrainView view = new ExtentTerrainView(editSession);
        int window = (int) Math.ceil(size) + 16;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * size;
            int x = position.x() + (int) Math.round(Math.cos(angle) * distance);
            int z = position.z() + (int) Math.round(Math.sin(angle) * distance);
            int ground = TerrainMath.surfaceNear(view, x, z, position.y(), window);
            if (ground != TerrainMath.NO_SURFACE) {
                place(editSession, x, ground, z, pattern, size, random);
            }
        }
    }
}
