package nl.gzmn.gZMNBuildtools.modules.roof.engine;

import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

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
        boolean flat = style.equals("flat");
        boolean hipish = style.equals("hip") || style.equals("pyramid");
        if (!flat && !hipish && !style.equals("gable")) {
            return new Plan(Map.of(), 0, "Unknown roof style '" + style + "'.");
        }
        boolean alongX = switch (options.axis()) {
            case "x" -> true;
            case "z" -> false;
            default -> bounds.sizeX() >= bounds.sizeZ();
        };
        int alongMin = alongX ? bounds.minX() : bounds.minZ();
        int alongMax = alongX ? bounds.maxX() : bounds.maxZ();
        int perpMin = alongX ? bounds.minZ() : bounds.minX();
        int perpMax = alongX ? bounds.maxZ() : bounds.maxX();
        int spanAlong = alongMax - alongMin + 1;
        int spanPerp = perpMax - perpMin + 1;
        double centerPerp = (perpMin + perpMax) / 2.0;
        double centerAlong = (alongMin + alongMax) / 2.0;
        double halfSpan = spanPerp / 2.0;
        double halfAlong = spanAlong / 2.0;
        int h = options.height() > 0 ? options.height() : Math.max(1, (int) Math.round(halfSpan));
        int ridgeLength = style.equals("pyramid") ? 0 : Math.max(0, spanAlong - spanPerp);
        Random random = new Random(options.seed());
        int overhang = options.overhang();

        Map<Cell, Part> placements = new LinkedHashMap<>();
        boolean anyTop = false;
        for (int a = alongMin - overhang; a <= alongMax + overhang; a++) {
            for (int p = perpMin - overhang; p <= perpMax + overhang; p++) {
                int x = alongX ? a : p;
                int z = alongX ? p : a;
                int base = tops.top(x, z);
                if (base < 0) {
                    base = tops.top(Math.max(bounds.minX(), Math.min(bounds.maxX(), x)),
                            Math.max(bounds.minZ(), Math.min(bounds.maxZ(), z)));
                }
                if (base < 0) {
                    continue;
                }
                anyTop = true;
                int eave = base + 1;
                double d = Math.abs(p - centerPerp);
                double dEnd = hipish ? Math.max(0, Math.abs(a - centerAlong) - ridgeLength / 2.0) : 0;
                int rise;
                if (flat) {
                    rise = 0;
                } else {
                    double n = hipish ? Math.max(d / halfSpan, dEnd / halfAlong) : d / halfSpan;
                    double jitter = options.noise() > 0 ? (random.nextDouble() * 2 - 1) * options.noise() : 0;
                    rise = Math.max(0, Math.min(h, (int) Math.round(h * (1 - n) + jitter)));
                    if (options.levelEnds() && (a == alongMin || a == alongMax)) {
                        rise = h;
                    }
                }
                int roofY = eave + rise;
                if (flat || rise == h) {
                    placements.put(new Cell(x, roofY, z), new Part(ShapeKind.SLAB, null, true));
                } else if (!hipish || d / halfSpan >= dEnd / halfAlong) {
                    placements.put(new Cell(x, roofY, z), new Part(ShapeKind.STAIRS, perpFacing(p, centerPerp, alongX), false));
                } else {
                    placements.put(new Cell(x, roofY, z), new Part(ShapeKind.STAIRS, alongFacing(a, centerAlong, alongX), false));
                }
                if (options.stackDown()) {
                    for (int y = eave; y < roofY; y++) {
                        placements.putIfAbsent(new Cell(x, y, z), new Part(ShapeKind.FULL, null, false));
                    }
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

    private static String alongFacing(int along, double center, boolean alongX) {
        if (alongX) {
            return along < center ? "west" : "east";
        }
        return along < center ? "north" : "south";
    }
}
