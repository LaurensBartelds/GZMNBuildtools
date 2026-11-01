package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

/** An inclusive block bounding box. */
public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static Bounds of(int x, int y, int z) {
        return new Bounds(x, y, z, x, y, z);
    }

    public Bounds include(int x, int y, int z) {
        return new Bounds(Math.min(minX, x), Math.min(minY, y), Math.min(minZ, z),
                Math.max(maxX, x), Math.max(maxY, y), Math.max(maxZ, z));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    @Override
    public String toString() {
        return sizeX() + "x" + sizeY() + "x" + sizeZ();
    }
}
