package nl.gzmn.gZMNBuildtools.integration.worldedit.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

/** Matches positions whose Y lies within [min, max] (inclusive). */
public class HeightMask extends AbstractMask {

    private final int min;
    private final int max;

    public HeightMask(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(BlockVector3 position) {
        return position.y() >= min && position.y() <= max;
    }

    @Override
    public Mask copy() {
        return this;
    }
}
