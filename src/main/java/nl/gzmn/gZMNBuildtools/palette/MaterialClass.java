package nl.gzmn.gZMNBuildtools.palette;

/**
 * Coarse material family of a block, so automatic palettes keep stone with
 * stone and wood with wood instead of matching on colour alone.
 */
public enum MaterialClass {
    WOOD, WOOL, CONCRETE, TERRACOTTA, METAL, EARTH, ORGANIC, STONE;

    public static MaterialClass of(String id) {
        String n = BlockColors.normalize(id);
        if (n.endsWith("_log") || n.endsWith("_wood") || n.endsWith("planks") || n.endsWith("_stem")
                || n.endsWith("_hyphae") || n.startsWith("bamboo")) {
            return WOOD;
        }
        if (n.endsWith("wool")) {
            return WOOL;
        }
        if (n.contains("concrete")) {
            return CONCRETE;
        }
        if (n.contains("terracotta")) {
            return TERRACOTTA;
        }
        if (n.contains("copper") || n.equals("iron_block") || n.equals("gold_block")
                || n.equals("netherite_block") || n.startsWith("raw_") || n.equals("diamond_block")
                || n.equals("emerald_block") || n.equals("lapis_block")) {
            return METAL;
        }
        if (n.contains("dirt") || n.equals("mud") || n.equals("clay") || n.equals("soul_soil")
                || n.equals("soul_sand") || n.equals("snow_block")) {
            return EARTH;
        }
        if (n.contains("wart") || n.contains("mushroom") || n.contains("moss") || n.equals("hay_block")
                || n.equals("dried_kelp_block") || n.contains("coral") || n.contains("ghast")
                || n.contains("resin") && !n.contains("brick")) {
            return ORGANIC;
        }
        return STONE;
    }
}
