package nl.gzmn.gZMNBuildtools.api;

import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.typereplace.BlockFamily;
import nl.gzmn.gZMNBuildtools.typereplace.MaterialDefinition;

import java.util.List;
import java.util.Set;

/**
 * Registry of block "families" (a base material plus its connecting variants:
 * stairs, slabs, walls, fences, …). Backed by {@code block-families.yml}; other
 * plugins can contribute families via {@link #register(String, MaterialDefinition)}.
 */
public interface BlockFamilyRegistry {

    /** Names of all known materials and groups (used for tab-completion). */
    Set<String> materialNames();

    /** Resolve a material name to its family (variants discovered against the world registry). */
    BlockFamily family(String name);

    boolean isGroup(String name);

    List<String> group(String name);

    /** The variant role ("stairs", "slab", …, or "block") of a block. */
    String variantType(BlockType blockType);

    /** Map a source block to the equivalent variant in a target family. */
    BlockType mapVariant(BlockType source, BlockFamily target);

    void register(String key, MaterialDefinition definition);

    /** Reload families from the backing source (no-op for in-memory registries). */
    void reload();
}
