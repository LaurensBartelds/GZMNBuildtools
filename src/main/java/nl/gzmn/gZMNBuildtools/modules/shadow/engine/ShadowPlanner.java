package nl.gzmn.gZMNBuildtools.modules.shadow.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Casts a shadow of a build: every exposed face block is projected along a
 * direction onto the first solid, non-build cell, which becomes the shadow.
 * Pure and deterministic.
 */
public final class ShadowPlanner {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** Answers whether a cell holds a non-air block. */
    @FunctionalInterface
    public interface SolidOracle {
        boolean solid(int x, int y, int z);
    }

    /** The shadow cells mapped to the build cell that cast them, or why there is none. */
    public record Plan(Map<Cell, Cell> shadows, int blockCount, String problem) {
    }

    private ShadowPlanner() {
    }

    /**
     * @param bounds the area the build lives in (used for nothing but sanity; sources come from {@code build})
     * @param build  the non-air build cells
     * @param solid  world solidity oracle
     * @param dx dy dz unit direction the shadow travels (exactly one non-zero component)
     * @param dist   how far a shadow may travel, in blocks
     * @param minY maxY world height limits; projections leaving them stop
     */
    public static Plan plan(Bounds bounds, Set<Cell> build, SolidOracle solid,
                            int dx, int dy, int dz, int dist, int minY, int maxY) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return new Plan(Map.of(), 0, "dir must be down, north, east, south or west.");
        }
        if (dist < 1) {
            return new Plan(Map.of(), 0, "dist must be at least 1.");
        }
        if (build.isEmpty()) {
            return new Plan(Map.of(), 0, "There is no build to cast a shadow from.");
        }
        Map<Cell, Cell> out = new LinkedHashMap<>();
        for (Cell source : exposedSources(build, dx, dy, dz)) {
            for (int t = 1; t <= dist; t++) {
                int x = source.x() + dx * t;
                int y = source.y() + dy * t;
                int z = source.z() + dz * t;
                if (y < minY || y > maxY) {
                    break;
                }
                Cell cell = new Cell(x, y, z);
                if (build.contains(cell)) {
                    continue;
                }
                if (solid.solid(x, y, z)) {
                    out.putIfAbsent(cell, source);
                    break;
                }
            }
        }
        return new Plan(out, out.size(), null);
    }

    /**
     * The blocks whose shadow matters: for a vertical direction the top of each
     * column, for a horizontal direction the face pointing away from the travel
     * direction, one per line perpendicular to it.
     */
    private static List<Cell> exposedSources(Set<Cell> build, int dx, int dy, int dz) {
        Map<List<Integer>, Cell> best = new LinkedHashMap<>();
        for (Cell c : build) {
            List<Integer> key = dy != 0 ? List.of(c.x(), c.z())
                    : (dx != 0 ? List.of(c.y(), c.z()) : List.of(c.x(), c.y()));
            int along = -(c.x() * dx + c.y() * dy + c.z() * dz);
            Cell cur = best.get(key);
            if (cur == null || along > -(cur.x() * dx + cur.y() * dy + cur.z() * dz)) {
                best.put(key, c);
            }
        }
        return new ArrayList<>(best.values());
    }
}
