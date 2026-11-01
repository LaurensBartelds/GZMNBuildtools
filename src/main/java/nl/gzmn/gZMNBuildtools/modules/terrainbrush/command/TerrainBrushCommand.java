package nl.gzmn.gZMNBuildtools.modules.terrainbrush.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.PlayerDataStore;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.TerrainBrushType;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.fawe.TerrainBrushFactory;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.worldedit.FaweBrushBinder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * {@code /terrainbrush}: binds one of the GZMN terrain brushes to the held item
 * as a regular FAWE brush, so {@code //size}, {@code //mat}, {@code //mask},
 * scroll actions and visualisation keep working.
 */
public class TerrainBrushCommand {

    public static final String PERMISSION = "gzmnbuildtools.terrainbrush";

    /** Saved data section (in players/<uuid>.yml). */
    private static final String DATA = "terrain-brushes";
    private static final java.util.regex.Pattern LOADOUT_NAME = java.util.regex.Pattern.compile("[a-z0-9_-]{1,32}");

    /** The brush a player bound most recently, so it can be saved as a loadout. */
    private record Bound(String type, int radius, String args) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PlayerDataStore playerData;
    private final Map<UUID, Bound> lastBound = new ConcurrentHashMap<>();

    public TerrainBrushCommand(JavaPlugin plugin, Messages messages, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.messages = messages;
        this.playerData = playerData;
    }

    public int maxRadius() {
        return Math.max(1, plugin.getConfig().getInt("terrain-brushes.max-radius", 32));
    }

    public void list(Player player) {
        messages.info(player, "Terrain brushes (click to fill in):");
        for (TerrainBrushType type : TerrainBrushType.values()) {
            player.sendMessage(Component.text("  " + type.id(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + type.description(), NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/terrainbrush " + type.id() + " 5 "))
                    .hoverEvent(HoverEvent.showText(Component.text(type.usage()))));
        }
    }

    public void help(Player player, String typeId) {
        Optional<TerrainBrushType> type = TerrainBrushType.byId(typeId);
        if (type.isEmpty()) {
            messages.error(player, "Unknown terrain brush '%s'.", typeId);
            list(player);
            return;
        }
        messages.info(player, "%s", type.get().usage());
        for (BrushParam p : type.get().params()) {
            messages.info(player, "  %s (default %s): %s%s", p.name(), p.display(p.defaultValue()),
                    p.description(), p.isChoice() ? " [" + String.join(", ", p.choices()) + "]" : "");
        }
    }

    public void bind(Player player, String typeId, int radius, String rest) {
        Optional<TerrainBrushType> type = TerrainBrushType.byId(typeId);
        if (type.isEmpty()) {
            messages.error(player, "Unknown terrain brush '%s'.", typeId);
            list(player);
            return;
        }
        if (radius > maxRadius()) {
            messages.error(player, "Radius %d is above the configured maximum of %d.", radius, maxRadius());
            return;
        }

        BrushArguments<TerrainBrushType> args;
        try {
            args = BrushArguments.parse(type.get(), rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        try {
            List<Pattern> patterns = new ArrayList<>();
            for (String text : args.patterns()) {
                patterns.add(parsePattern(actor, session, text));
            }
            Pattern fill = patterns.isEmpty() ? null : patterns.get(0);

            FaweBrushBinder.bind(player, TerrainBrushFactory.create(args, patterns), PERMISSION, radius,
                    ("gzmn " + typeId + " " + radius + " " + (rest == null ? "" : rest)).trim(), fill);
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to bind terrain brush for " + player.getName(), e);
            messages.error(player, "Could not bind the brush; see the server log for details.");
            return;
        }

        lastBound.put(player.getUniqueId(), new Bound(type.get().id(), radius, rest == null ? "" : rest.trim()));
        messages.success(player, "Bound %s brush (radius %d%s) to your held item. %s",
                type.get().id(), radius, args.patterns().isEmpty() ? "" : ", " + String.join(" + ", args.patterns()),
                args.describe());
        messages.info(player, "Use //size, //mask and //mat to adjust it, /tb save <name> to keep it, or /none to unbind.");
    }

    // ---- loadouts ----------------------------------------------------------------------------------------------

    public List<String> loadoutNames(Player player) {
        return List.copyOf(playerData.keys(player.getUniqueId(), DATA, "loadouts"));
    }

    /** Saves the brush the player bound last under a name. */
    public void saveLoadout(Player player, String rawName) {
        Bound bound = lastBound.get(player.getUniqueId());
        if (bound == null) {
            messages.error(player, "Bind a terrain brush first (/tb), then save it.");
            return;
        }
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!LOADOUT_NAME.matcher(name).matches()) {
            messages.error(player, "Loadout names use letters, digits, - and _ (up to 32).");
            return;
        }
        int max = Math.max(1, plugin.getConfig().getInt("loadouts.max-per-player", 30));
        if (!loadoutNames(player).contains(name) && loadoutNames(player).size() >= max) {
            messages.error(player, "You already have %d terrain loadouts; delete one first.", max);
            return;
        }
        playerData.set(player.getUniqueId(), DATA, "loadouts." + name,
                Map.of("type", bound.type(), "radius", bound.radius(), "args", bound.args()));
        messages.success(player, "Saved your %s brush as '%s'. /tb load %s binds it again.", bound.type(), name, name);
    }

    /** Binds a saved terrain brush to the held item. */
    public void loadLoadout(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Object type = playerData.get(player.getUniqueId(), DATA, "loadouts." + name + ".type");
        if (type == null) {
            messages.error(player, "You have no terrain loadout called '%s'.", rawName);
            loadouts(player);
            return;
        }
        Object radius = playerData.get(player.getUniqueId(), DATA, "loadouts." + name + ".radius");
        Object args = playerData.get(player.getUniqueId(), DATA, "loadouts." + name + ".args");
        int r = radius instanceof Number n ? n.intValue() : 5;
        bind(player, type.toString(), Math.min(r, maxRadius()), args == null ? "" : args.toString());
    }

    public void deleteLoadout(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!loadoutNames(player).contains(name)) {
            messages.error(player, "You have no terrain loadout called '%s'.", rawName);
            return;
        }
        playerData.set(player.getUniqueId(), DATA, "loadouts." + name, null);
        messages.success(player, "Deleted terrain loadout '%s'.", name);
    }

    public void loadouts(Player player) {
        List<String> names = loadoutNames(player);
        if (names.isEmpty()) {
            messages.info(player, "No terrain loadouts yet. Bind a brush, then /tb save <name>.");
            return;
        }
        messages.info(player, "Your terrain loadouts (click to bind to your held item):");
        for (String name : names) {
            String base = "loadouts." + name + ".";
            String summary = playerData.get(player.getUniqueId(), DATA, base + "type") + " "
                    + playerData.get(player.getUniqueId(), DATA, base + "radius") + " "
                    + playerData.get(player.getUniqueId(), DATA, base + "args");
            player.sendMessage(Component.text("  " + name, NamedTextColor.AQUA)
                    .append(Component.text(" " + summary.trim(), NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand("/tb load " + name))
                    .append(Component.text("  [delete]", NamedTextColor.DARK_GRAY)
                            .clickEvent(ClickEvent.suggestCommand("/tb delete " + name))));
        }
    }

    private Pattern parsePattern(com.sk89q.worldedit.entity.Player actor, LocalSession session, String input)
            throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(actor.getWorld());
        context.setExtent(actor.getWorld());
        context.setSession(session);
        context.setRestricted(true);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, context);
    }

    public List<String> typeSuggestions() {
        return Arrays.stream(TerrainBrushType.values()).map(TerrainBrushType::id).toList();
    }

    /** Suggestions for the free-form argument tail (pattern and name=value parameters). */
    public List<String> argumentSuggestions(String typeId, String typed) {
        Optional<TerrainBrushType> type = TerrainBrushType.byId(typeId);
        if (type.isEmpty()) {
            return List.of();
        }
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        int words = prefix.isEmpty() ? 0 : prefix.trim().split(" ").length;
        if (words < type.get().patternSlots()) {
            List<String> options = type.get() == TerrainBrushType.TREE
                    ? (words == 0 ? List.of("oak_log", "spruce_log", "birch_log", "dark_oak_log", "cherry_log")
                    : List.of("oak_leaves", "spruce_leaves", "birch_leaves", "azalea_leaves", "cherry_leaves"))
                    : List.of("stone", "andesite", "mossy_cobblestone", "dirt", "grass_block",
                    "#byslope[0:grass_block][40:stone]");
            for (String block : options) {
                if (block.startsWith(current)) {
                    out.add(prefix + block);
                }
            }
            return out;
        }
        for (BrushParam p : type.get().params()) {
            String option = p.name() + "=";
            if (current.startsWith(option) && p.isChoice()) {
                p.choices().stream().map(c -> option + c).filter(c -> c.startsWith(current))
                        .forEach(c -> out.add(prefix + c));
            } else if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
