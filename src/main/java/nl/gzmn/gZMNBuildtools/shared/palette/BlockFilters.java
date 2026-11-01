package nl.gzmn.gZMNBuildtools.shared.palette;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Name-based block filters used to keep generated palettes to plain building blocks. */
public final class BlockFilters {

    /**
     * Blocks never picked by automatic palettes: functional blocks, light
     * sources, falling blocks, ores, patterned or biome-tinted blocks. Glob
     * syntax ({@code *} = anything). Overridable with {@code palettes.exclude}.
     */
    public static final List<String> DEFAULT_PALETTE_EXCLUDES = List.of(
            "*_ore", "infested_*", "*_concrete_powder", "sand", "red_sand", "gravel", "suspicious_*",
            "*command_block", "structure_*", "jigsaw", "test_*", "*spawner", "vault", "tnt",
            "*_glazed_terracotta", "barrier", "light", "bedrock", "budding_amethyst", "reinforced_deepslate",
            "end_portal_frame", "moving_piston", "piston", "sticky_piston", "observer", "dispenser",
            "dropper", "crafter", "target", "note_block", "jukebox", "redstone_block", "redstone_lamp",
            "*_table", "loom", "barrel", "*furnace", "smoker", "beehive", "bee_nest", "*shulker_box",
            "*chest", "*bookshelf", "*_shelf", "respawn_anchor", "lodestone", "beacon", "sculk*",
            "decorated_pot", "heavy_core", "creaking_heart", "pumpkin", "carved_pumpkin", "jack_o_lantern",
            "melon", "sponge", "wet_sponge", "slime_block", "honey_block", "*ice", "powder_snow",
            "*_leaves", "*froglight", "glowstone", "sea_lantern", "shroomlight", "crying_obsidian",
            "magma_block", "*copper_bulb", "*copper_grate", "grass_block", "podzol", "mycelium",
            "trial_*", "*item_frame", "cactus", "chiseled_*", "comparator", "repeater", "*_head", "*_skull", "chiseled_*bookshelf",
            // Live coral blocks die out of water.
            "brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block", "tube_coral_block");

    /** Suffixes of blocks that are not full cubes (shape variants and decorations). */
    private static final Pattern PARTIAL = Pattern.compile(
            ".*(_stairs|_slab|_wall|_fence|_fence_gate|_button|_pressure_plate|_door|_trapdoor|_sign"
                    + "|_carpet|_pane|_bars|_banner|_bed|_candle|_rail|_torch|_lantern|_sapling|_flower"
                    + "|_coral|_coral_fan|_plate|_chain|_rod|_bulb)$");

    private BlockFilters() {
    }

    /** True for ids matching any of the glob patterns. */
    public static Predicate<String> anyGlob(List<String> globs) {
        List<Pattern> patterns = globs.stream()
                .map(g -> Pattern.compile(g.toLowerCase(Locale.ROOT).replace(".", "\\.").replace("*", ".*")))
                .toList();
        return id -> {
            String normalized = BlockColors.normalize(id);
            return patterns.stream().anyMatch(p -> p.matcher(normalized).matches());
        };
    }

    /** Name-only guess at whether a block is a partial shape; the runtime uses real block data. */
    public static boolean looksPartial(String id) {
        return PARTIAL.matcher(BlockColors.normalize(id)).matches();
    }
}
