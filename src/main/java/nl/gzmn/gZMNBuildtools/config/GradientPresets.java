package nl.gzmn.gZMNBuildtools.config;

import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;


public final class GradientPresets {

        private static final Map<String, GradientPreset> PRESETS = new LinkedHashMap<>();

        static {
                
                register(GradientPreset.builder("grayscale_light")
                                .displayName("Light Grayscale")
                                .description("White to gray gradient")
                                .icon(Material.WHITE_CONCRETE)
                                .category(GradientPreset.PresetCategory.GRAYSCALE)
                                .blocks("white_concrete", "light_gray_concrete", "gray_concrete")
                                .build());

                register(GradientPreset.builder("grayscale_full")
                                .displayName("Full Grayscale")
                                .description("White to black gradient")
                                .icon(Material.GRAY_CONCRETE)
                                .category(GradientPreset.PresetCategory.GRAYSCALE)
                                .blocks("white_concrete", "light_gray_concrete", "gray_concrete", "black_concrete")
                                .build());

                register(GradientPreset.builder("grayscale_wool")
                                .displayName("Wool Grayscale")
                                .description("Soft wool gradient")
                                .icon(Material.GRAY_WOOL)
                                .category(GradientPreset.PresetCategory.GRAYSCALE)
                                .blocks("white_wool", "light_gray_wool", "gray_wool", "black_wool")
                                .build());

                
                register(GradientPreset.builder("warm_sunset")
                                .displayName("Sunset")
                                .description("Yellow to red sunset colors")
                                .icon(Material.ORANGE_CONCRETE)
                                .category(GradientPreset.PresetCategory.WARM)
                                .blocks("yellow_concrete", "orange_concrete", "red_concrete")
                                .build());

                register(GradientPreset.builder("warm_fire")
                                .displayName("Fire")
                                .description("Fire-like gradient with embers")
                                .icon(Material.ORANGE_WOOL)
                                .category(GradientPreset.PresetCategory.WARM)
                                .blocks("yellow_concrete", "orange_concrete", "red_concrete", "black_concrete")
                                .build());

                register(GradientPreset.builder("warm_autumn")
                                .displayName("Autumn")
                                .description("Fall foliage colors")
                                .icon(Material.ORANGE_TERRACOTTA)
                                .category(GradientPreset.PresetCategory.WARM)
                                .blocks("yellow_terracotta", "orange_terracotta", "red_terracotta", "brown_terracotta")
                                .build());

                
                register(GradientPreset.builder("cool_ocean")
                                .displayName("Ocean")
                                .description("Light to deep ocean blues")
                                .icon(Material.BLUE_CONCRETE)
                                .category(GradientPreset.PresetCategory.COOL)
                                .blocks("light_blue_concrete", "cyan_concrete", "blue_concrete")
                                .build());

                register(GradientPreset.builder("cool_ice")
                                .displayName("Ice")
                                .description("Frozen ice gradient")
                                .icon(Material.PACKED_ICE)
                                .category(GradientPreset.PresetCategory.COOL)
                                .blocks("snow_block", "packed_ice", "blue_ice")
                                .build());

                register(GradientPreset.builder("cool_twilight")
                                .displayName("Twilight")
                                .description("Blue to purple evening sky")
                                .icon(Material.PURPLE_CONCRETE)
                                .category(GradientPreset.PresetCategory.COOL)
                                .blocks("light_blue_concrete", "blue_concrete", "purple_concrete", "black_concrete")
                                .build());

                
                register(GradientPreset.builder("nature_forest")
                                .displayName("Forest")
                                .description("Green forest canopy")
                                .icon(Material.GREEN_CONCRETE)
                                .category(GradientPreset.PresetCategory.NATURE)
                                .blocks("lime_concrete", "green_concrete", "brown_concrete")
                                .build());

                register(GradientPreset.builder("nature_grass")
                                .displayName("Grass Blend")
                                .description("Natural terrain gradient")
                                .icon(Material.GRASS_BLOCK)
                                .category(GradientPreset.PresetCategory.NATURE)
                                .blocks("grass_block", "dirt", "coarse_dirt")
                                .build());

                register(GradientPreset.builder("nature_moss")
                                .displayName("Mossy")
                                .description("Moss and vegetation")
                                .icon(Material.MOSS_BLOCK)
                                .category(GradientPreset.PresetCategory.NATURE)
                                .blocks("moss_block", "moss_carpet", "grass_block", "dirt")
                                .build());

                
                register(GradientPreset.builder("earth_desert")
                                .displayName("Desert Sand")
                                .description("Sandy desert colors")
                                .icon(Material.SAND)
                                .category(GradientPreset.PresetCategory.EARTH)
                                .blocks("sandstone", "sand", "red_sand", "red_sandstone")
                                .build());

                register(GradientPreset.builder("earth_terrain")
                                .displayName("Terrain")
                                .description("Natural terrain blend")
                                .icon(Material.DIRT)
                                .category(GradientPreset.PresetCategory.EARTH)
                                .blocks("grass_block", "dirt", "coarse_dirt", "gravel", "stone")
                                .build());

                register(GradientPreset.builder("earth_clay")
                                .displayName("Clay & Terracotta")
                                .description("Natural clay colors")
                                .icon(Material.TERRACOTTA)
                                .category(GradientPreset.PresetCategory.EARTH)
                                .blocks("white_terracotta", "light_gray_terracotta", "terracotta", "brown_terracotta")
                                .build());

                
                register(GradientPreset.builder("stone_weathered")
                                .displayName("Weathered Stone")
                                .description("Stone to mossy cobblestone")
                                .icon(Material.COBBLESTONE)
                                .category(GradientPreset.PresetCategory.STONE)
                                .blocks("stone", "cobblestone", "mossy_cobblestone")
                                .build());

                register(GradientPreset.builder("stone_depth")
                                .displayName("Depth Stone")
                                .description("Surface to deep stone")
                                .icon(Material.DEEPSLATE)
                                .category(GradientPreset.PresetCategory.STONE)
                                .blocks("stone", "andesite", "deepslate", "cobbled_deepslate")
                                .build());

                register(GradientPreset.builder("stone_brick")
                                .displayName("Brick Weathering")
                                .description("Stone brick deterioration")
                                .icon(Material.STONE_BRICKS)
                                .category(GradientPreset.PresetCategory.STONE)
                                .blocks("stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks")
                                .build());
        }

        private static void register(GradientPreset preset) {
                PRESETS.put(preset.getId(), preset);
        }

        
        public static GradientPreset get(String id) {
                return PRESETS.get(id);
        }

        
        public static List<GradientPreset> getByCategory(GradientPreset.PresetCategory category) {
                return PRESETS.values().stream()
                                .filter(p -> p.getCategory() == category)
                                .collect(Collectors.toList());
        }

        
        public static List<GradientPreset> getAll() {
                return new ArrayList<>(PRESETS.values());
        }

        
        public static List<String> getAllIds() {
                return new ArrayList<>(PRESETS.keySet());
        }

        
        public static boolean exists(String id) {
                return PRESETS.containsKey(id);
        }

        
        public static int count() {
                return PRESETS.size();
        }

        private GradientPresets() {
        }
}
