package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.brush.ColumnShift;
import nl.gzmn.gZMNBuildtools.brush.HeightField;
import nl.gzmn.gZMNBuildtools.brush.HeightOperation;
import nl.gzmn.gZMNBuildtools.integration.worldedit.terrain.ExtentTerrainView;

/**
 * Reshapes terrain by moving each column's surface up or down as computed by a
 * {@link HeightOperation}, carrying the surface layers and decorations along.
 */
public class HeightmapBrush implements Brush {

    private final HeightOperation operation;

    public HeightmapBrush(HeightOperation operation) {
        this.operation = operation;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        int radius = Math.max(1, (int) Math.round(size));
        HeightField field = HeightField.sample(new ExtentTerrainView(editSession),
                position.x(), position.y(), position.z(), radius);
        int[] deltas = operation.deltas(field);
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();
        BlockState air = BlockTypes.AIR.getDefaultState();

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int delta = deltas[field.index(dx, dz)];
                if (delta == 0 || !field.hasSurface(dx, dz)) {
                    continue;
                }
                int x = position.x() + dx;
                int z = position.z() + dz;
                ColumnShift shift = ColumnShift.plan(field.height(dx, dz), delta, minY, maxY);

                // Read every source block before writing so the shift never reads its own output.
                int readFrom = shift.minSourceY();
                BlockState[] original = new BlockState[shift.maxSourceY() - readFrom + 1];
                for (int i = 0; i < original.length; i++) {
                    original[i] = editSession.getBlock(x, readFrom + i, z);
                }
                for (int y = shift.fromY(); y <= shift.toY(); y++) {
                    int source = shift.sourceFor(y);
                    editSession.setBlock(x, y, z, source == ColumnShift.AIR ? air : original[source - readFrom]);
                }
            }
        }
    }
}
