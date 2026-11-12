package nl.gzmn.gZMNBuildtools.modules.roof.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a region and the per-column wall-top heights into the blocks of a roof.
 * Pure: no world access, deterministic for a fixed seed.
 */
public final class RoofPlanner {

    /** One block position of the roof. */
    public record Cell(int x, int y, int z) {
    }

    /** What to place at a cell: a shape kind of the pattern, a facing for stairs, and the slab half. */
    public record Part(ShapeKind kind, String facing, boolean topHalf) {
    }

    /** The roof as placements, or why there is none. */
    public record Plan(Map<Cell, Part> placements, int blockCount, String problem) {
    }

    /** The top wall block of a column, or -1 when the column has no build block. */
    @FunctionalInterface
    public interface ColumnTops {
        int top(int x, int z);
    }

    private RoofPlanner() {
    }

    public static Plan plan(Bounds bounds, ColumnTops tops, RoofOptions options) {
        if (bounds.sizeX() < 3 || bounds.sizeZ() < 3) {
            return new Plan(Map.of(), 0, "Select a bigger area (at least 3 blocks wide) to put a roof on.");
        }
        String style = options.style();
        if (!style.equals("gable") && !style.equals("flat")) {
            return new Plan(Map.of(), 0, "That roof style is not available yet.");
        }
        boolean alongX = switch (options.axis()) {
            case "x" -> true;
            case "z" -> false;
            default -> bounds.sizeX() >= bounds.sizeZ();
        };
        int perpMin = alongX ? bounds.minZ() : bounds.minX();
        int perpMax = alongX ? bounds.maxZ() : bounds.maxX();
        int spanPerp = perpMax - perpMin + 1;
        double center = (perpMin + perpMax) / 2.0;
        double halfSpan = spanPerp / 2.0;
        int h = options.height() > 0 ? options.height() : Math.max(1, (int) Math.round(halfSpan));

        Map<Cell, Part> placements = new LinkedHashMap<>();
        boolean anyTop = false;
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                int base = tops.top(x, z);
                if (base < 0) {
                    continue;
                }
                anyTop = true;
                int perp = alongX ? z : x;
                int eave = base + 1;
                if (style.equals("flat")) {
                    placements.put(new Cell(x, eave, z), new Part(ShapeKind.SLAB, null, true));
                    continue;
                }
                double d = Math.abs(perp - center);
                int rise = Math.max(0, Math.min(h, (int) Math.round(h * (1 - d / halfSpan))));
                int roofY = eave + rise;
                if (rise == h) {
                    placements.put(new Cell(x, roofY, z), new Part(ShapeKind.SLAB, null, true));
                } else {
                    placements.put(new Cell(x, roofY, z), new Part(ShapeKind.STAIRS, perpFacing(perp, center, alongX), false));
                }
            }
        }
        if (!anyTop) {
            return new Plan(Map.of(), 0, "No build found there to put a roof on.");
        }
        return new Plan(placements, placements.size(), null);
    }

    private static String perpFacing(int perp, double center, boolean alongX) {
        if (alongX) {
            return perp < center ? "north" : "south";
        }
        return perp < center ? "west" : "east";
    }
}
