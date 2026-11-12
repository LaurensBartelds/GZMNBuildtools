package nl.gzmn.gZMNBuildtools.modules.revolve.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Revolves a cuboid selection around an axis: every cell is copied at each
 * step's angle around the axis, remembering which source cell and how many
 * quarter turns each copy has. Pure and deterministic.
 */
public final class RevolvePlanner {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** Where an output cell comes from and how far it is turned (0..3 quarter turns). */
    public record Placement(Cell source, int quarterTurns) {
    }

    /** The revolve as output cell to placement, or why there is none. */
    public record Plan(Map<Cell, Placement> placements, int blockCount, String problem) {
    }

    private RevolvePlanner() {
    }

    /**
     * @param sel     the cuboid selection to copy
     * @param center  a point on the axis (selection point 1)
     * @param axis    'x', 'y' or 'z'
     * @param degrees total angle; each step turns degrees/steps further
     * @param steps   number of copies (including the unturned one)
     */
    public static Plan plan(Bounds sel, int[] center, char axis, double degrees, int steps) {
        Map<Cell, Placement> out = new LinkedHashMap<>();
        boolean any = false;
        for (int x = sel.minX(); x <= sel.maxX(); x++) {
            for (int y = sel.minY(); y <= sel.maxY(); y++) {
                for (int z = sel.minZ(); z <= sel.maxZ(); z++) {
                    any = true;
                    for (int i = 0; i < steps; i++) {
                        double angle = degrees * i / steps;
                        int[] p = rotate(x, y, z, center, axis, Math.toRadians(angle));
                        out.put(new Cell(p[0], p[1], p[2]),
                                new Placement(new Cell(x, y, z), quarterTurns(angle)));
                    }
                }
            }
        }
        if (!any) {
            return new Plan(Map.of(), 0, "Select a cuboid area first.");
        }
        return new Plan(out, out.size(), null);
    }

    /** Right-handed rotation of (x,y,z) around the axis through center by theta radians. */
    private static int[] rotate(int x, int y, int z, int[] center, char axis, double theta) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double dx = x - center[0];
        double dy = y - center[1];
        double dz = z - center[2];
        double nx;
        double ny;
        double nz;
        switch (axis) {
            case 'x' -> {
                ny = center[1] + dy * cos - dz * sin;
                nz = center[2] + dy * sin + dz * cos;
                nx = x;
            }
            case 'z' -> {
                nx = center[0] + dx * cos - dy * sin;
                ny = center[1] + dx * sin + dy * cos;
                nz = z;
            }
            default -> {
                nx = center[0] + dx * cos + dz * sin;
                nz = center[2] - dx * sin + dz * cos;
                ny = y;
            }
        }
        return new int[]{(int) Math.round(nx), (int) Math.round(ny), (int) Math.round(nz)};
    }

    private static int quarterTurns(double degrees) {
        int q = (int) Math.round(degrees / 90.0);
        return ((q % 4) + 4) % 4;
    }
}
