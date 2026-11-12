package nl.gzmn.gZMNBuildtools.modules.smear.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smears the leading surface of a brushed sphere along a direction: every
 * line of cells perpendicular to the direction contributes its leading cell,
 * which is copied {@code amount} blocks further along (negative amounts drag
 * it back). Pure and deterministic.
 */
public final class SmearEngine {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** Reads the block id at a position, or null for air. */
    @FunctionalInterface
    public interface BlockGrid {
        String blockAt(int x, int y, int z);
    }

    /** The smear as target cell to source cell. */
    public record Plan(Map<Cell, Cell> changes, int blockCount) {
    }

    private SmearEngine() {
    }

    /**
     * @param grid   the world blocks
     * @param center brush centre
     * @param radius brush radius
     * @param dir    unit direction vector (one axis is 1 or -1, the rest 0)
     * @param amount how many blocks to extend along dir; negative drags back
     */
    public static Plan smear(BlockGrid grid, int[] center, double radius, int[] dir, int amount) {
        Map<Cell, Cell> changes = new LinkedHashMap<>();
        Map<Long, Cell> leadByLine = new LinkedHashMap<>();
        int r = (int) Math.ceil(radius);
        for (int x = center[0] - r; x <= center[0] + r; x++) {
            for (int y = center[1] - r; y <= center[1] + r; y++) {
                for (int z = center[2] - r; z <= center[2] + r; z++) {
                    double dx = x - center[0];
                    double dy = y - center[1];
                    double dz = z - center[2];
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    if (grid.blockAt(x, y, z) == null) {
                        continue;
                    }
                    long line = lineKey(x, y, z, dir);
                    int proj = x * dir[0] + y * dir[1] + z * dir[2];
                    Cell cell = new Cell(x, y, z);
                    Cell lead = leadByLine.get(line);
                    if (lead == null || proj > lead.x() * dir[0] + lead.y() * dir[1] + lead.z() * dir[2]) {
                        leadByLine.put(line, cell);
                    }
                }
            }
        }
        for (Cell lead : leadByLine.values()) {
            int steps = Math.abs(amount);
            for (int k = 1; k <= steps; k++) {
                int s = amount >= 0 ? k : -k;
                changes.put(new Cell(lead.x() + dir[0] * s, lead.y() + dir[1] * s, lead.z() + dir[2] * s), lead);
            }
        }
        return new Plan(changes, changes.size());
    }

    /** Identifies the line of cells parallel to dir through (x,y,z): the coords perpendicular to dir. */
    private static long lineKey(int x, int y, int z, int[] dir) {
        long a;
        long b;
        if (dir[0] != 0) {
            a = y;
            b = z;
        } else if (dir[1] != 0) {
            a = x;
            b = z;
        } else {
            a = x;
            b = y;
        }
        return (a << 32) | (b & 0xffffffffL);
    }
}
