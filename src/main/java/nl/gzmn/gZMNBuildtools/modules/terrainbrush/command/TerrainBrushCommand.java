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
import java.util.Optional;
import java.util.logging.Level;

/**
 * {@code /terrainbrush}: binds one of the GZMN terrain brushes to the held item
 * as a regular FAWE brush, so {@code //size}, {@code //mat}, {@code //mask},
 * scroll actions and visualisation keep working.
 */
public class TerrainBrushCommand {

    public static final String PERMISSION = "gzmnbuildtools.terrainbrush";

    private final JavaPlugin plugin;
    private final Messages messages;

    public TerrainBrushCommand(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
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

        messages.success(player, "Bound %s brush (radius %d%s) to your held item. %s",
                type.get().id(), radius, args.patterns().isEmpty() ? "" : ", " + String.join(" + ", args.patterns()),
                args.describe());
        messages.info(player, "Use //size, //mask and //mat to adjust it, or /none to unbind.");
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
