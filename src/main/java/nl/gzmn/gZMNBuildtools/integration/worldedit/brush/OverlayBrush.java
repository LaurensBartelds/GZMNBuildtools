package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;

/**
 * Paints the top {@code depth} solid blocks of every upward-facing surface
 * inside the brush cylinder (so overhangs and ledges are painted too).
 */
public class OverlayBrush implements Brush {

    private final int depth;

    public OverlayBrush(int depth) {
        this.depth = depth;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        int radius = Math.max(1, (int) Math.round(size));
        ExtentTerrainView view = new ExtentTerrainView(editSession);
        int top = Math.min(view.maxY(), position.y() + radius);
        int bottom = Math.max(view.minY(), position.y() - radius);
        double r2 = (radius + 0.5) * (radius + 0.5);

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                int x = position.x() + dx;
                int z = position.z() + dz;
                boolean aboveSolid = top < view.maxY() && view.isSolid(x, top + 1, z);
                int y = top;
                while (y >= bottom) {
                    boolean solid = view.isSolid(x, y, z);
                    if (solid && !aboveSolid) {
                        int painted = 0;
                        while (y >= bottom && painted < depth && view.isSolid(x, y, z)) {
                            editSession.setBlock(x, y, z, pattern);
                            painted++;
                            y--;
                        }
                        aboveSolid = true;
                        continue;
                    }
                    aboveSolid = solid;
                    y--;
                }
            }
        }
    }
}
