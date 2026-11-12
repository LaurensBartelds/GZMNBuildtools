package nl.gzmn.gZMNBuildtools.modules.resize.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resizes a cuboid selection by nearest-neighbour sampling: every output cell
 * remembers which source cell it was taken from. Pure and deterministic.
 */
public final class ResizePlanner {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** The resized selection as output cell to source cell, or why there is none. */
    public record Plan(Map<Cell, Cell> placements, int blockCount, Bounds out, String problem) {
    }

    private ResizePlanner() {
    }

    /**
     * @param sel the cuboid selection to resize; the output is anchored at its minimum corner
     * @param sx  scale factors per axis (each >= 0.25); the output size is round(size * scale), at least 1
     */
    public static Plan plan(Bounds sel, double sx, double sy, double sz) {
        if (sel.sizeX() <= 0 || sel.sizeY() <= 0 || sel.sizeZ() <= 0) {
            return new Plan(Map.of(), 0, null, "Select a cuboid area first.");
        }
        int ox = Math.max(1, (int) Math.round(sel.sizeX() * sx));
        int oy = Math.max(1, (int) Math.round(sel.sizeY() * sy));
        int oz = Math.max(1, (int) Math.round(sel.sizeZ() * sz));
        Map<Cell, Cell> out = new LinkedHashMap<>();
        for (int x = 0; x < ox; x++) {
            for (int y = 0; y < oy; y++) {
                for (int z = 0; z < oz; z++) {
                    int sxCell = sel.minX() + Math.min(sel.sizeX() - 1, (int) (x / sx));
                    int syCell = sel.minY() + Math.min(sel.sizeY() - 1, (int) (y / sy));
                    int szCell = sel.minZ() + Math.min(sel.sizeZ() - 1, (int) (z / sz));
                    out.put(new Cell(sel.minX() + x, sel.minY() + y, sel.minZ() + z),
                            new Cell(sxCell, syCell, szCell));
                }
            }
        }
        Bounds outBounds = new Bounds(sel.minX(), sel.minY(), sel.minZ(), sel.minX() + ox - 1,
                sel.minY() + oy - 1, sel.minZ() + oz - 1);
        return new Plan(out, out.size(), outBounds, null);
    }
}
