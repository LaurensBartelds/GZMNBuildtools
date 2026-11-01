package nl.gzmn.gZMNBuildtools.modules.terrainbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import nl.gzmn.gZMNBuildtools.shared.shapes.Rocks;

import java.util.Random;

/** Places boulders embedded in the ground; scattered rocks vary in size up to the radius. */
public class RockBrush extends ScatterBrush {

    private final double flatness;
    private final double roughness;
    private final double sink;
    private final boolean scattered;

    public RockBrush(double flatness, double roughness, double sink, int count) {
        super(count);
        this.flatness = flatness;
        this.roughness = roughness;
        this.sink = sink;
        this.scattered = count > 1;
    }

    @Override
    protected void place(EditSession editSession, int x, int groundY, int z, Pattern pattern, double size,
                         Random random) {
        double radius = scattered ? Math.max(1, size * (0.25 + 0.35 * random.nextDouble())) : Math.max(1, size);
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();
        Rocks.generate(x, groundY, z, radius, flatness, roughness, sink, random.nextLong(), (bx, by, bz) -> {
            if (by >= minY && by <= maxY) {
                editSession.setBlock(bx, by, bz, pattern);
            }
        });
    }
}
