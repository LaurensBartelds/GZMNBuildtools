package nl.gzmn.gZMNBuildtools.modules.scatter.prefab;

import nl.gzmn.gZMNBuildtools.modules.scatter.engine.Footprint;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;

/** Something the scatter brush can place: a schematic, or a procedural tree or rock. */
public interface Prefab {

    String name();

    Footprint footprint();

    /** Bottom layers that are the prefab's own ground (a dirt base under a tree), buried by default. */
    int groundLayers();

    /**
     * Sends the prefab's blocks with its anchor (the middle of its bottom layer)
     * at (x, y, z), turned and mirrored like {@link Footprint#base(int, boolean)}.
     */
    void emit(int x, int y, int z, int turns, boolean flip, long seed, BuildEditor.StateSink sink);
}
