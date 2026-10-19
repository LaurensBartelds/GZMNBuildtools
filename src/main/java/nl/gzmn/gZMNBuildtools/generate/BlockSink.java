package nl.gzmn.gZMNBuildtools.generate;

/** Receives the block positions of a generated shape. */
@FunctionalInterface
public interface BlockSink {
    void accept(int x, int y, int z);
}
