package nl.gzmn.gZMNBuildtools.shared.shapes;

/**
 * Receives generated blocks tagged with which pattern to use (0 = first) and,
 * for log-like parts, the axis they run along ('x', 'y', 'z' or 0 for none).
 */
@FunctionalInterface
public interface PartSink {
    void accept(int x, int y, int z, int pattern, char axis);
}
