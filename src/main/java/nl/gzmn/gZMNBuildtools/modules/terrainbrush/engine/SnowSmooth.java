package nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Smooths snow cover: each column's {@code snow_layer} thickness is moved towards
 * the average thickness of the brush disc around it. Pure and deterministic.
 */
public final class SnowSmooth {

    /** One block position. */
    public record Cell(int x, int y, int z) {
    }

    /** The y of a column's top solid block, or below {@code minY} when the column is empty. */
    @FunctionalInterface
    public interface ColumnTop {
        int topY(int x, int z);
    }

    /** The {@code layers} value of the snow on top of a column's top solid block; 0 when there is none. */
    @FunctionalInterface
    public interface ColumnSnow {
        int layers(int x, int z);
    }

    /** Cells to set to a snow layer count, cells to clear, or how many blocks change in total. */
    public record Plan(Map<Cell, Integer> newLayers, Set<Cell> remove, int blockCount) {
    }

    private SnowSmooth() {
    }

    /**
     * @param cx cz    centre of the brush disc
     * @param radius   brush radius
     * @param top      column tops of the world
     * @param snow     current snow layers per column
     * @param strength fraction of the difference to the neighbourhood average applied per click (0.05-1)
     * @param fill     when true, bare ground whose neighbourhood averages at least half a layer gains snow
     * @param minY maxY world height limits
     */
    public static Plan plan(int cx, int cz, int radius, ColumnTop top, ColumnSnow snow,
                            double strength, boolean fill, int minY, int maxY) {
        double r2 = (radius + 0.5) * (radius + 0.5);
        Map<Cell, Integer> newLayers = new LinkedHashMap<>();
        Set<Cell> remove = new LinkedHashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                int topY = top.topY(x, z);
                if (topY < minY || topY >= maxY) {
                    continue;
                }
                int current = snow.layers(x, z);
                double avg = average(cx, cz, radius, snow);
                int target;
                if (fill && current == 0 && avg >= 0.5) {
                    target = Math.max(1, (int) Math.round(avg));
                } else {
                    target = clamp((int) Math.round(current + (avg - current) * strength));
                }
                if (target == current) {
                    continue;
                }
                Cell cell = new Cell(x, topY + 1, z);
                if (target == 0) {
                    remove.add(cell);
                } else {
                    newLayers.put(cell, target);
                }
            }
        }
        return new Plan(newLayers, remove, newLayers.size() + remove.size());
    }

    private static double average(int cx, int cz, int radius, ColumnSnow snow) {
        double r2 = (radius + 0.5) * (radius + 0.5);
        long sum = 0;
        int count = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                sum += snow.layers(cx + dx, cz + dz);
                count++;
            }
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    private static int clamp(int layers) {
        return Math.max(0, Math.min(8, layers));
    }
}
