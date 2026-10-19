package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;

import java.util.List;

/**
 * Matches solid blocks that are exposed (non-solid neighbour) on at least one
 * of the given faces, e.g. {@code #facing[up]} for top surfaces or
 * {@code #facing[north,south]} for walls facing those directions.
 */
public class FacingMask extends AbstractExtentMask {

    private final List<Direction> faces;

    public FacingMask(Extent extent, List<Direction> faces) {
        super(extent);
        this.faces = List.copyOf(faces);
    }

    @Override
    public boolean test(BlockVector3 position) {
        return test(getExtent(), position);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        if (!SolidBlockMask.isSolid(extent.getBlock(position.x(), position.y(), position.z()))) {
            return false;
        }
        for (Direction face : faces) {
            BlockVector3 offset = face.toBlockVector();
            int y = position.y() + offset.y();
            if (y < extent.getMinY() || y > extent.getMaxY()) {
                continue;
            }
            if (!SolidBlockMask.isSolid(extent.getBlock(position.x() + offset.x(), y, position.z() + offset.z()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Mask copy() {
        return new FacingMask(getExtent(), faces);
    }
}
