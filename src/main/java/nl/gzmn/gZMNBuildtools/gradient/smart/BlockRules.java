package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Set;
import java.util.function.Predicate;

/**
 * What kind of block something is for repainting purposes: a building block
 * (full cube or stairs/slab/wall/fence), a protected detail (glass, lights,
 * containers, doors, plants, ...) or natural terrain.
 */
public final class BlockRules {

    /** Ground the build scan should not wander into unless it is the material clicked. */
    public static final Set<String> TERRAIN = Set.of(
            "grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "dirt_path", "farmland",
            "mud", "clay", "sand", "red_sand", "gravel", "stone", "deepslate", "tuff", "granite", "diorite",
            "andesite", "calcite", "dripstone_block", "netherrack", "end_stone", "snow_block", "snow", "ice",
            "packed_ice", "blue_ice", "bedrock", "soul_sand", "soul_soil", "basalt", "blackstone", "moss_block",
            "sandstone", "red_sandstone", "water", "lava", "short_grass", "tall_grass");

    private final BlockCatalog catalog;
    private final Predicate<String> detail;

    /**
     * @param detail full-cube blocks that must never be repainted (lights,
     *               containers, translucent blocks, liquids, ...)
     */
    public BlockRules(BlockCatalog catalog, Predicate<String> detail) {
        this.catalog = catalog;
        this.detail = detail;
    }

    public BlockCatalog catalog() {
        return catalog;
    }

    public boolean isStructural(String id) {
        BlockCatalog.Info info = catalog.classify(id);
        return info.kind() != ShapeKind.OTHER && !detail.test(info.id());
    }

    public boolean isTerrain(String id) {
        String n = catalog.classify(id).id();
        return TERRAIN.contains(n) || n.endsWith("_ore");
    }

    public String group(String id) {
        return catalog.classify(id).group();
    }
}
