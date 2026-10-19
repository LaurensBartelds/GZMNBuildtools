package nl.gzmn.gZMNBuildtools.brush;

import nl.gzmn.gZMNBuildtools.terrain.TerrainMath;
import nl.gzmn.gZMNBuildtools.terrain.TerrainView;

/**
 * Surface heights of the (2r+1)^2 columns around a brush centre. Columns
 * without a surface in the search window hold {@link TerrainMath#NO_SURFACE}.
 */
public final class HeightField {

    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final int size;
    private final int[] heights;

    public HeightField(int centerX, int centerY, int centerZ, int radius, int[] heights) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.size = radius * 2 + 1;
        if (heights.length != size * size) {
            throw new IllegalArgumentException("Expected " + size * size + " heights");
        }
        this.heights = heights;
    }

    public static HeightField sample(TerrainView view, int x, int y, int z, int radius) {
        int size = radius * 2 + 1;
        int window = Math.max(8, radius * 2);
        int[] heights = new int[size * size];
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                heights[(dz + radius) * size + dx + radius] = TerrainMath.surfaceNear(view, x + dx, z + dz, y, window);
            }
        }
        return new HeightField(x, y, z, radius, heights);
    }

    public int radius() {
        return radius;
    }

    public int size() {
        return size;
    }

    public int centerX() {
        return centerX;
    }

    public int centerY() {
        return centerY;
    }

    public int centerZ() {
        return centerZ;
    }

    /** Height at offset (dx, dz) from the centre; NO_SURFACE outside the field. */
    public int height(int dx, int dz) {
        if (Math.abs(dx) > radius || Math.abs(dz) > radius) {
            return TerrainMath.NO_SURFACE;
        }
        return heights[index(dx, dz)];
    }

    public boolean hasSurface(int dx, int dz) {
        return height(dx, dz) != TerrainMath.NO_SURFACE;
    }

    public int index(int dx, int dz) {
        return (dz + radius) * size + dx + radius;
    }

    /** Height of the centre column, or the clicked Y when it has no surface. */
    public int centerHeight() {
        int h = height(0, 0);
        return h == TerrainMath.NO_SURFACE ? centerY : h;
    }
}
