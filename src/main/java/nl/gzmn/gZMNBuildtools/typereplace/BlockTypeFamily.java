package nl.gzmn.gZMNBuildtools.typereplace;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.*;

public class BlockTypeFamily {

    private static final String[] VARIANT_SUFFIXES = {
            "_stairs", "_slab", "_wall", "_fence", "_fence_gate", "_bars", "_grate"
    };

    private static final Map<String, MaterialDefinition> MATERIAL_DEFINITIONS = new HashMap<>();

    private static final Map<String, List<String>> MATERIAL_GROUPS = new HashMap<>();

    private static final Map<String, String> REVERSE_VARIANT_LOOKUP = new HashMap<>();

    static {
        register("stone").base("stone", "stone").stairs().slab().wall().build();
        register("cobblestone").base("cobblestone", "cobblestone").stairs().slab().wall().build();
        register("mossy_cobblestone").base("mossy_cobblestone", "mossy_cobblestone").stairs().slab().wall().build();
        register("stone_brick").base("stone_bricks", "stone_brick").stairs().slab().wall().build();
        register("mossy_stone_brick").base("mossy_stone_bricks", "mossy_stone_brick").stairs().slab().wall().build();
        register("granite").base("granite", "granite").stairs().slab().wall().build();
        register("polished_granite").base("polished_granite", "polished_granite").stairs().slab().wall().build();
        register("diorite").base("diorite", "diorite").stairs().slab().wall().build();
        register("polished_diorite").base("polished_diorite", "polished_diorite").stairs().slab().wall().build();
        register("andesite").base("andesite", "andesite").stairs().slab().wall().build();
        register("polished_andesite").base("polished_andesite", "polished_andesite").stairs().slab().wall().build();

        register("deepslate").base("deepslate", "deepslate").build();
        register("cobbled_deepslate").base("cobbled_deepslate", "cobbled_deepslate").stairs().slab().wall().build();
        register("polished_deepslate").base("polished_deepslate", "polished_deepslate").stairs().slab().wall().build();
        register("deepslate_brick").base("deepslate_bricks", "deepslate_brick").stairs().slab().wall().build();
        register("deepslate_tile").base("deepslate_tiles", "deepslate_tile").stairs().slab().wall().build();

        register("brick").base("bricks", "brick").stairs().slab().wall().build();
        register("mud_brick").base("mud_bricks", "mud_brick").stairs().slab().wall().build();

        register("sandstone").base("sandstone", "sandstone").stairs().slab().wall().build();
        register("smooth_sandstone").base("smooth_sandstone", "smooth_sandstone").stairs().slab().build();
        register("cut_sandstone").base("cut_sandstone", "cut_sandstone").slab().build();
        register("red_sandstone").base("red_sandstone", "red_sandstone").stairs().slab().wall().build();
        register("smooth_red_sandstone").base("smooth_red_sandstone", "smooth_red_sandstone").stairs().slab().build();
        register("cut_red_sandstone").base("cut_red_red_sandstone", "cut_red_sandstone").slab().build();

        register("prismarine").base("prismarine", "prismarine").stairs().slab().wall().build();
        register("prismarine_brick").base("prismarine_bricks", "prismarine_brick").stairs().slab().build();
        register("dark_prismarine").base("dark_prismarine", "dark_prismarine").stairs().slab().build();

        register("nether_brick").base("nether_bricks", "nether_brick").stairs().slab().wall().fence().build();
        register("red_nether_brick").base("red_nether_bricks", "red_nether_brick").stairs().slab().wall().build();
        register("blackstone").base("blackstone", "blackstone").stairs().slab().wall().build();
        register("polished_blackstone").base("polished_blackstone", "polished_blackstone").stairs().slab().wall()
                .build();
        register("polished_blackstone_brick").base("polished_blackstone_bricks", "polished_blackstone_brick").stairs()
                .slab().wall().build();

        register("quartz").base("quartz_block", "quartz").stairs().slab().build();
        register("smooth_quartz").base("smooth_quartz", "smooth_quartz").stairs().slab().build();

        register("end_stone_brick").base("end_stone_bricks", "end_stone_brick").stairs().slab().wall().build();
        register("purpur").base("purpur_block", "purpur").stairs().slab().build();

        for (String wood : new String[] { "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
                "cherry", "bamboo", "crimson", "warped" }) {
            String planksName = wood.equals("bamboo") ? "bamboo_planks" : wood + "_planks";
            register(wood).base(planksName, wood).stairs().slab().fence().fenceGate().build();
        }

        register("copper").base("copper_block", "cut_copper").stairs().slab().bars("copper_bars").grate("copper_grate")
                .build();
        register("cut_copper").base("cut_copper", "cut_copper").stairs().slab().bars("copper_bars")
                .grate("copper_grate").build();
        register("exposed_copper").base("exposed_copper", "exposed_cut_copper").stairs().slab()
                .bars("exposed_copper_bars").grate("exposed_copper_grate").build();
        register("exposed_cut_copper").base("exposed_cut_copper", "exposed_cut_copper").stairs().slab()
                .bars("exposed_copper_bars").grate("exposed_copper_grate").build();
        register("weathered_copper").base("weathered_copper", "weathered_cut_copper").stairs().slab()
                .bars("weathered_copper_bars").grate("weathered_copper_grate").build();
        register("weathered_cut_copper").base("weathered_cut_copper", "weathered_cut_copper").stairs().slab()
                .bars("weathered_copper_bars").grate("weathered_copper_grate").build();
        register("oxidized_copper").base("oxidized_copper", "oxidized_cut_copper").stairs().slab()
                .bars("oxidized_copper_bars").grate("oxidized_copper_grate").build();
        register("oxidized_cut_copper").base("oxidized_cut_copper", "oxidized_cut_copper").stairs().slab()
                .bars("oxidized_copper_bars").grate("oxidized_copper_grate").build();

        register("waxed_copper").base("waxed_copper_block", "waxed_cut_copper").stairs().slab()
                .bars("waxed_copper_bars").grate("waxed_copper_grate").build();
        register("waxed_cut_copper").base("waxed_cut_copper", "waxed_cut_copper").stairs().slab()
                .bars("waxed_cut_copper_bars").grate("waxed_cut_copper_grate").build();
        register("waxed_exposed_copper").base("waxed_exposed_copper", "waxed_exposed_cut_copper").stairs().slab()
                .bars("waxed_exposed_copper_bars").grate("waxed_exposed_copper_grate").build();
        register("waxed_exposed_cut_copper").base("waxed_exposed_cut_copper", "waxed_exposed_cut_copper").stairs()
                .slab().bars("waxed_exposed_copper_bars").grate("waxed_exposed_cut_copper_grate").build();
        register("waxed_weathered_copper").base("waxed_weathered_copper", "waxed_weathered_cut_copper").stairs().slab()
                .bars("waxed_weathered_copper_bars").grate("waxed_weathered_copper_grate").build();
        register("waxed_weathered_cut_copper").base("waxed_weathered_cut_copper", "waxed_weathered_cut_copper").stairs()
                .slab().bars("waxed_weathered_copper_bars").grate("waxed_weathered_copper_grate").build();
        register("waxed_oxidized_copper").base("waxed_oxidized_copper", "waxed_oxidized_cut_copper").stairs().slab()
                .bars("waxed_oxidized_copper_bars").grate("waxed_oxidized_copper_grate").build();
        register("waxed_oxidized_cut_copper").base("waxed_oxidized_cut_copper", "waxed_oxidized_cut_copper").stairs()
                .slab().bars("waxed_oxidized_copper_bars").grate("waxed_oxidized_copper_grate").build();

        register("iron").base("iron_block", "iron").bars().build();

        register("tuff").base("tuff", "tuff").stairs().slab().wall().build();
        register("polished_tuff").base("polished_tuff", "polished_tuff").stairs().slab().wall().build();
        register("tuff_brick").base("tuff_bricks", "tuff_brick").stairs().slab().wall().build();
    }

    public static Set<String> getAllMaterialNames() {
        Set<String> keys = new HashSet<>(MATERIAL_DEFINITIONS.keySet());
        keys.addAll(MATERIAL_GROUPS.keySet());
        return keys;
    }

    public static MaterialFamilyBuilder register(String key) {
        return new MaterialFamilyBuilder(key);
    }

    public static class MaterialFamilyBuilder {
        private final String key;
        private String blockName;
        private String variantPrefix;
        private boolean hasStairs, hasSlab, hasWall, hasFence, hasFenceGate, hasBars, hasGrate;
        private String grateName;
        private String barsName;

        private MaterialFamilyBuilder(String key) {
            this.key = key;
            this.blockName = key;
            this.variantPrefix = key;
        }

        public MaterialFamilyBuilder base(String blockName, String variantPrefix) {
            this.blockName = blockName;
            this.variantPrefix = variantPrefix;
            return this;
        }

        public MaterialFamilyBuilder stairs() {
            this.hasStairs = true;
            return this;
        }

        public MaterialFamilyBuilder slab() {
            this.hasSlab = true;
            return this;
        }

        public MaterialFamilyBuilder wall() {
            this.hasWall = true;
            return this;
        }

        public MaterialFamilyBuilder fence() {
            this.hasFence = true;
            return this;
        }

        public MaterialFamilyBuilder fenceGate() {
            this.hasFenceGate = true;
            return this;
        }

        public MaterialFamilyBuilder bars() {
            this.hasBars = true;
            return this;
        }

        public MaterialFamilyBuilder bars(String name) {
            this.hasBars = true;
            this.barsName = name;
            return this;
        }

        public MaterialFamilyBuilder grate(String name) {
            this.hasGrate = true;
            this.grateName = name;
            return this;
        }

        public void build() {
            MATERIAL_DEFINITIONS.put(key.toLowerCase(), new MaterialDefinition(blockName, variantPrefix,
                    hasStairs, hasSlab, hasWall, hasFence, hasFenceGate, hasBars, hasGrate, grateName, barsName));
        }
    }

    private final String baseMaterial;
    private final Map<String, BlockType> variants;

    public BlockTypeFamily(String baseMaterial) {
        this.baseMaterial = baseMaterial.toLowerCase().replace("minecraft:", "");
        this.variants = new HashMap<>();
        discoverVariants();
    }

    private void discoverVariants() {
        MaterialDefinition def = MATERIAL_DEFINITIONS.get(baseMaterial);

        if (def != null) {
            addVariantIfExists("block", def.blockName);
            if (def.hasStairs)
                addVariantIfExists("stairs", def.variantPrefix + "_stairs");
            if (def.hasSlab)
                addVariantIfExists("slab", def.variantPrefix + "_slab");
            if (def.hasWall)
                addVariantIfExists("wall", def.variantPrefix + "_wall");
            if (def.hasFence)
                addVariantIfExists("fence", def.variantPrefix + "_fence");
            if (def.hasFenceGate)
                addVariantIfExists("fence_gate", def.variantPrefix + "_fence_gate");
            if (def.hasBars) {
                if (def.barsName != null) {
                    addVariantIfExists("bars", def.barsName);
                } else {
                    addVariantIfExists("bars", def.variantPrefix + "_bars");
                }
            }
            if (def.hasGrate && def.grateName != null)
                addVariantIfExists("grate", def.grateName);
        } else {
            discoverGenericVariants();
        }
    }

    private void discoverGenericVariants() {
        addVariantIfExists("block", baseMaterial);
        addVariantIfExists("block", baseMaterial + "s");
        addVariantIfExists("block", baseMaterial + "_block");
        addVariantIfExists("block", baseMaterial + "_planks");

        addVariantIfExists("stairs", baseMaterial + "_stairs");
        addVariantIfExists("slab", baseMaterial + "_slab");
        addVariantIfExists("wall", baseMaterial + "_wall");
        addVariantIfExists("fence", baseMaterial + "_fence");
        addVariantIfExists("fence_gate", baseMaterial + "_fence_gate");
        addVariantIfExists("bars", baseMaterial + "_bars");
        addVariantIfExists("grate", baseMaterial + "_grate");
    }

    private void addVariantIfExists(String variantType, String blockId) {
        if (variants.containsKey(variantType))
            return;

        BlockType block = BlockTypes.get("minecraft:" + blockId);
        if (block != null) {
            variants.put(variantType, block);
            REVERSE_VARIANT_LOOKUP.putIfAbsent(block.id(), variantType);
        }
    }

    public static String getVariantType(BlockType blockType) {
        if (REVERSE_VARIANT_LOOKUP.containsKey(blockType.id())) {
            return REVERSE_VARIANT_LOOKUP.get(blockType.id());
        }

        String id = blockType.id().toLowerCase();

        if (id.endsWith("_stairs"))
            return "stairs";
        if (id.endsWith("_slab"))
            return "slab";
        if (id.endsWith("_wall"))
            return "wall";
        if (id.endsWith("_fence_gate"))
            return "fence_gate";
        if (id.endsWith("_fence"))
            return "fence";
        if (id.endsWith("_bars"))
            return "bars";
        if (id.endsWith("_grate"))
            return "grate";

        return "block";
    }

    public BlockType getVariant(String variantType) {
        return variants.get(variantType);
    }

    public boolean hasVariant(String variantType) {
        return variants.containsKey(variantType);
    }

    public Map<String, BlockType> getVariants() {
        return Collections.unmodifiableMap(variants);
    }

    public String getBaseMaterial() {
        return baseMaterial;
    }

    private static boolean isVerticalConnectorVariant(String variantType) {
        return variantType.equals("wall") || variantType.equals("fence") || variantType.equals("bars");
    }

    private static BlockType findVerticalConnectorVariant(BlockTypeFamily targetFamily) {
        BlockType variant = targetFamily.getVariant("wall");
        if (variant != null)
            return variant;

        variant = targetFamily.getVariant("fence");
        if (variant != null)
            return variant;

        variant = targetFamily.getVariant("bars");
        if (variant != null)
            return variant;

        String baseMaterial = targetFamily.baseMaterial;
        variant = BlockTypes.get("minecraft:" + baseMaterial + "_wall");
        if (variant != null)
            return variant;

        variant = BlockTypes.get("minecraft:" + baseMaterial + "_fence");
        if (variant != null)
            return variant;

        variant = BlockTypes.get("minecraft:" + baseMaterial + "_bars");
        return variant;
    }

    public static BlockType mapVariant(BlockType sourceBlock, BlockTypeFamily targetFamily) {
        String variantType = getVariantType(sourceBlock);

        if (isVerticalConnectorVariant(variantType)) {
            BlockType targetVariant = findVerticalConnectorVariant(targetFamily);
            if (targetVariant == null) {
                targetVariant = targetFamily.getVariant("block");
            }
            return targetVariant;
        }

        if (variantType.equals("fence_gate")) {
            return targetFamily.getVariant("fence_gate");
        }

        if (variantType.equals("grate")) {
            return targetFamily.getVariant("grate");
        }

        BlockType targetVariant = targetFamily.getVariant(variantType);

        if (targetVariant == null) {
            targetVariant = targetFamily.getVariant("block");
        }

        return targetVariant;
    }

    public static boolean isMaterialGroup(String material) {
        return MATERIAL_GROUPS.containsKey(material.toLowerCase());
    }

    public static List<String> getMaterialGroup(String groupName) {
        return MATERIAL_GROUPS.getOrDefault(groupName.toLowerCase(), Collections.emptyList());
    }

    public static Set<String> getMaterialGroupNames() {
        return Collections.unmodifiableSet(MATERIAL_GROUPS.keySet());
    }

    @Override
    public String toString() {
        return "BlockTypeFamily{" +
                "baseMaterial='" + baseMaterial + '\'' +
                ", variants=" + variants.keySet() +
                '}';
    }

    private static class MaterialDefinition {
        final String blockName;
        final String variantPrefix;
        final boolean hasStairs;
        final boolean hasSlab;
        final boolean hasWall;
        final boolean hasFence;
        final boolean hasFenceGate;
        final boolean hasBars;
        final boolean hasGrate;
        final String grateName;
        final String barsName;

        MaterialDefinition(String blockName, String variantPrefix, boolean hasStairs, boolean hasSlab,
                boolean hasWall, boolean hasFence, boolean hasFenceGate,
                boolean hasBars, boolean hasGrate, String grateName, String barsName) {
            this.blockName = blockName;
            this.variantPrefix = variantPrefix;
            this.hasStairs = hasStairs;
            this.hasSlab = hasSlab;
            this.hasWall = hasWall;
            this.hasFence = hasFence;
            this.hasFenceGate = hasFenceGate;
            this.hasBars = hasBars;
            this.hasGrate = hasGrate;
            this.grateName = grateName;
            this.barsName = barsName;
        }
    }
}
