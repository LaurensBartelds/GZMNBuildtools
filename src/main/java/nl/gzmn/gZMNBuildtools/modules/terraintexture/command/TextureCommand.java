package nl.gzmn.gZMNBuildtools.modules.terraintexture.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.TexturePreset;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.fawe.TextureBrush;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.fawe.TextureRunner;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.FaweBrushBinder;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * {@code /gztexture}: repaint the terrain in a selection, or with a brush, using
 * a preset that picks blocks by slope, height, depth and noise patches.
 */
public class TextureCommand {

    public static final String PERMISSION = "gzmnbuildtools.texture";

    /** What the player asked for after the preset name. */
    record Options(boolean preview, Long seed, Integer radius) {

        static Options parse(List<String> tokens) {
            boolean preview = false;
            Long seed = null;
            Integer radius = null;
            for (String token : tokens) {
                String t = token.toLowerCase(Locale.ROOT);
                try {
                    if (t.equals("-p") || t.equals("preview")) {
                        preview = true;
                    } else if (t.startsWith("seed=")) {
                        seed = Long.parseLong(t.substring(5));
                    } else if (t.startsWith("radius=")) {
                        radius = Integer.parseInt(t.substring(7));
                    } else if (t.matches("\\d+")) {
                        radius = Integer.parseInt(t);
                    } else {
                        throw new IllegalArgumentException("Unknown option '" + token + "' (use -p, seed=<n>, a radius)");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + token + "' needs a whole number");
                }
            }
            return new Options(preview, seed, radius);
        }
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final TextureRunner runner;
    private final PreviewService previews;
    private final Limits limits;

    public TextureCommand(JavaPlugin plugin, Messages messages, TextureRunner runner, PreviewService previews,
                          Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.runner = runner;
        this.previews = previews;
        this.limits = limits;
    }

    private long maxVolume(Player player) {
        return limits.get(player, Limits.Kind.SELECTION,
                Math.max(1, plugin.getConfig().getInt("terrain-texture.max-volume", 8_000_000)));
    }

    private int maxRadius(Player player) {
        return limits.get(player, Limits.Kind.RADIUS,
                Math.max(1, plugin.getConfig().getInt("terrain-texture.max-radius", 32)));
    }

    public void list(Player player) {
        if (runner.presets().ids().isEmpty()) {
            messages.error(player, "No terrain presets are loaded; check terrain-presets.yml.");
            return;
        }
        messages.info(player, "Terrain presets (click a name to texture your selection, or [brush] to paint by hand):");
        for (TexturePreset preset : runner.presets().byId().values()) {
            player.sendMessage(Component.text("  " + preset.id(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + preset.description(), NamedTextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(Component.text(String.join(", ", preset.blocks()))))
                    .clickEvent(ClickEvent.suggestCommand("/gztexture " + preset.id() + " -p"))
                    .append(Component.text("  [brush]", NamedTextColor.DARK_AQUA)
                            .clickEvent(ClickEvent.runCommand("/gztexture brush " + preset.id()))));
        }
        messages.info(player, "/gztexture <preset> [-p] textures the //wand selection; -p previews first.");
    }

    /** Dispatches the greedy argument string. */
    public void run(Player player, String input) {
        List<String> tokens = new ArrayList<>(Arrays.asList(input.trim().split("\\s+")));
        if (tokens.isEmpty() || tokens.get(0).isEmpty() || tokens.get(0).equalsIgnoreCase("list")) {
            list(player);
            return;
        }
        boolean brush = tokens.get(0).equalsIgnoreCase("brush");
        if (brush) {
            tokens.remove(0);
            if (tokens.isEmpty()) {
                messages.error(player, "Usage: /gztexture brush <preset> [radius] [seed=<n>]");
                return;
            }
        }
        String id = tokens.remove(0);
        Optional<TexturePreset> preset = runner.presets().get(id);
        if (preset.isEmpty()) {
            messages.error(player, "Unknown terrain preset '%s'.", id);
            list(player);
            return;
        }
        Options options;
        try {
            options = Options.parse(tokens);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        long seed = options.seed() != null ? options.seed() : TextureRunner.defaultSeed(preset.get());
        if (brush) {
            bind(player, preset.get(), options.radius() == null ? 6 : options.radius(), seed);
        } else {
            texture(player, preset.get(), options.preview(), seed);
        }
    }

    private void bind(Player player, TexturePreset preset, int radius, long seed) {
        if (radius > maxRadius(player)) {
            messages.error(player, "Radius %d is above your limit of %d.", radius, maxRadius(player));
            return;
        }
        UUID owner = player.getUniqueId();
        try {
            FaweBrushBinder.bind(player, new TextureBrush(runner, preset, seed, changed -> {
                Player online = plugin.getServer().getPlayer(owner);
                if (online != null) {
                    online.sendActionBar(Component.text(changed + " blocks textured (" + preset.id() + ")",
                            NamedTextColor.GRAY));
                }
            }), PERMISSION, radius, maxRadius(player), "gzmn texture " + preset.id(), null);
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        }
        messages.success(player, "Bound the %s texture brush (radius %d). Paint over terrain; builds are left alone.",
                preset.id(), radius);
        messages.info(player, "/size changes the radius and //mask limits it further. /none unbinds.");
    }

    private void texture(Player player, TexturePreset preset, boolean preview, long seed) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        Region region;
        try {
            region = session.getSelection(actor.getWorld());
        } catch (IncompleteRegionException e) {
            messages.error(player, "Select the area to texture first (//wand, //pos1 and //pos2).");
            return;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        Bounds bounds = new Bounds(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        long volume = (long) bounds.sizeX() * bounds.sizeY() * bounds.sizeZ();
        if (volume > maxVolume(player)) {
            messages.error(player, "That selection has %d blocks, above your limit of %d.", volume, maxVolume(player));
            return;
        }
        boolean cuboid = region instanceof CuboidRegion;
        Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
        PlayerEdits.async(player, editSession -> {
            int changed = runner.run(editSession, preset, seed, bounds,
                    cuboid ? (x, y, z) -> true : (x, y, z) -> region.contains(BlockVector3.at(x, y, z)),
                    preview ? (x, y, z, state) -> changes.put(BlockVector3.at(x, y, z), state)
                            : (x, y, z, state) -> editSession.setBlock(x, y, z, state));
            if (preview) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes),
                        "the " + preset.id() + " texture", () -> apply(player, changes)));
                return false;
            }
            if (changed == 0) {
                messages.info(player, "Nothing to texture: no natural terrain surface in the selection.");
                return false;
            }
            messages.success(player, "Textured the %s selection with %s: %d blocks changed. //undo reverts it.",
                    bounds.toString(), preset.id(), changed);
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Texturing failed for " + player.getName(), e);
            messages.error(player, "Texturing failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }

    public List<String> suggestions(String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        String[] words = prefix.trim().isEmpty() ? new String[0] : prefix.trim().split("\\s+");
        List<String> options = new ArrayList<>();
        if (words.length == 0) {
            options.addAll(runner.presets().ids());
            options.add("brush");
            options.add("list");
        } else if (words.length == 1 && words[0].equalsIgnoreCase("brush")) {
            options.addAll(runner.presets().ids());
        } else if (words[0].equalsIgnoreCase("brush")) {
            options.addAll(List.of("6", "10", "seed="));
        } else {
            options.addAll(List.of("-p", "seed="));
        }
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(current) && !(" " + typed + " ").contains(" " + option + " ")) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
