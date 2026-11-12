package nl.gzmn.gZMNBuildtools.modules.fixconnect.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reconnects fences, walls, panes and iron bars: each block's
 * north/east/south/west connection is set to whether the neighbour in that
 * direction is connectable (same family, or a solid full cube). Pure and
 * deterministic.
 */
public final class FixConnectPlanner {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** The block id at a position, or null for air. */
    @FunctionalInterface
    public interface BlockOracle {
        String id(int x, int y, int z);
    }

    /** A block's current connection properties as {north, east, south, west}. */
    @FunctionalInterface
    public interface ConnectionOracle {
        boolean[] connected(int x, int y, int z);
    }

    /** Cells whose connections change, mapped to the wanted {north, east, south, west}. */
    public record Plan(Map<Cell, boolean[]> changes, int blockCount, String problem) {
    }

    /** Direction indices used by {@link Plan}: north, east, south, west. */
    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] NAMES = {"north", "east", "south", "west"};

    private FixConnectPlanner() {
    }

    public static String[] directionNames() {
        return NAMES.clone();
    }

    /**
     * The connectable family of a block id: "fence", "wall", "pane" (panes and
     * iron bars), or null when the block has no connection properties
     * (fence gates, doors, plain blocks, ...).
     */
    public static String familyOf(String id) {
        if (id == null) {
            return null;
        }
        if (id.endsWith("_fence_gate") || id.endsWith("_fence")) {
            return id.endsWith("_fence_gate") ? null : "fence";
        }
        if (id.endsWith("_wall")) {
            return "wall";
        }
        if (id.endsWith("_pane") || id.equals("iron_bars")) {
            return "pane";
        }
        return null;
    }

    /**
     * @param bounds   the selection to fix
     * @param blocks   block ids of the world (null = air)
     * @param fullCube whether a block id is a solid full cube
     * @param current  current connection properties per cell
     */
    public static Plan plan(Bounds bounds, BlockOracle blocks, Predicate<String> fullCube,
                            ConnectionOracle current) {
        Map<Cell, boolean[]> changes = new LinkedHashMap<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    String family = familyOf(blocks.id(x, y, z));
                    if (family == null) {
                        continue;
                    }
                    boolean[] have = current.connected(x, y, z);
                    boolean[] want = new boolean[4];
                    boolean differs = false;
                    for (int d = 0; d < 4; d++) {
                        String neighbour = blocks.id(x + DIRS[d][0], y, z + DIRS[d][1]);
                        want[d] = neighbour != null && (family.equals(familyOf(neighbour)) || fullCube.test(neighbour));
                        if (have == null || have[d] != want[d]) {
                            differs = true;
                        }
                    }
                    if (differs) {
                        changes.put(new Cell(x, y, z), want);
                    }
                }
            }
        }
        return new Plan(changes, changes.size(), null);
    }
}
