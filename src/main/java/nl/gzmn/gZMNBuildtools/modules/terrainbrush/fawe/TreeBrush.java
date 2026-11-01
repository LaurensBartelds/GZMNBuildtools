package nl.gzmn.gZMNBuildtools.modules.terrainbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockState;
import nl.gzmn.gZMNBuildtools.shared.shapes.TreeStyle;
import nl.gzmn.gZMNBuildtools.shared.shapes.Trees;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PartPlacer;

import java.util.List;
import java.util.Random;

/** Plants procedural trees; the brush pattern is the log, the second pattern the leaves. */
public class TreeBrush extends ScatterBrush {

    private final TreeStyle style;
    private final int height;
    private final int thickness;
    private final int leafSize;
    private final Pattern leaves;

    public TreeBrush(TreeStyle style, int height, int thickness, int leafSize, int count, Pattern leaves) {
        super(count);
        this.style = style;
        this.height = height;
        this.thickness = thickness;
        this.leafSize = leafSize;
        this.leaves = leaves;
    }

    @Override
    protected void place(EditSession editSession, int x, int groundY, int z, Pattern pattern, double size,
                         Random random) {
        PartPlacer placer = new PartPlacer(List.of(pattern, leaves));
        // Vary scattered trees a little so a forest does not look copy-pasted.
        int h = Math.max(3, (int) Math.round(height * (0.8 + 0.4 * random.nextDouble())));
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();
        Trees.generate(style, x, groundY + 1, z, h, thickness, leafSize, random.nextLong(),
                (bx, by, bz, part, axis) -> {
                    if (by >= minY && by <= maxY) {
                        BlockState state = placer.state(bx, by, bz, part, axis);
                        if (state != null) {
                            editSession.setBlock(bx, by, bz, state);
                        }
                    }
                });
    }
}
