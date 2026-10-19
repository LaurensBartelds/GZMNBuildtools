package nl.gzmn.gZMNBuildtools.command.brush;

import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.brush.BrushArguments;
import nl.gzmn.gZMNBuildtools.brush.BrushParam;
import nl.gzmn.gZMNBuildtools.brush.TerrainBrushType;
import nl.gzmn.gZMNBuildtools.integration.worldedit.brush.TerrainBrushFactory;
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
            messages.info(player, "  %s (default %s): %s", p.name(), format(p.defaultValue()), p.description());
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

        BrushArguments args;
        try {
            args = BrushArguments.parse(type.get(), rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        try {
            Pattern fill = null;
            if (args.pattern() != null) {
                fill = parsePattern(actor, session, args.pattern());
            }

            BrushSettings settings = new BrushSettings();
            settings.setBrush(TerrainBrushFactory.create(args));
            settings.addPermission(PERMISSION);
            settings.addSetting(BrushSettings.SettingType.BRUSH,
                    ("gzmn " + typeId + " " + radius + " " + (rest == null ? "" : rest)).trim());

            session.setTool(actor, null);
            BrushTool tool = session.getBrushTool(actor);
            tool.setPrimary(settings);
            tool.setSecondary(settings);
            tool.setSize(radius);
            if (fill != null) {
                tool.setFill(fill);
            }
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
                type.get().id(), radius, args.pattern() == null ? "" : ", pattern " + args.pattern(),
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
        if (type.get().usesPattern() && prefix.isEmpty()) {
            for (String block : List.of("stone", "andesite", "mossy_cobblestone", "dirt", "grass_block",
                    "#byslope[0:grass_block][40:stone]")) {
                if (block.startsWith(current)) {
                    out.add(block);
                }
            }
            return out;
        }
        for (BrushParam p : type.get().params()) {
            String option = p.name() + "=";
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
