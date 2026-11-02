package nl.gzmn.gZMNBuildtools.modules.scatter.command;

import com.sk89q.worldedit.WorldEditException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.ScatterOptions;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.ScatterPlanner;
import nl.gzmn.gZMNBuildtools.modules.scatter.fawe.ScatterBrush;
import nl.gzmn.gZMNBuildtools.modules.scatter.prefab.Prefab;
import nl.gzmn.gZMNBuildtools.modules.scatter.prefab.PrefabLibrary;
import nl.gzmn.gZMNBuildtools.modules.scatter.prefab.ProceduralPrefab;
import nl.gzmn.gZMNBuildtools.shared.worldedit.FaweBrushBinder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** {@code /gzscatter}: bind a brush that scatters prefabs (schematics, trees, rocks) over the ground. */
public class ScatterCommand {

    public static final String PERMISSION = "gzmnbuildtools.scatter";

    private final JavaPlugin plugin;
    private final Messages messages;
    private final Supplier<PrefabLibrary> library;
    private final ScatterPlanner.Rules rules;

    public ScatterCommand(JavaPlugin plugin, Messages messages, Supplier<PrefabLibrary> library,
                          ScatterPlanner.Rules rules) {
        this.plugin = plugin;
        this.messages = messages;
        this.library = library;
        this.rules = rules;
    }

    private int maxRadius() {
        return Math.max(1, plugin.getConfig().getInt("scatter.max-radius", 32));
    }

    private int maxBlocks() {
        return Math.max(1, plugin.getConfig().getInt("scatter.max-blocks-per-click", 50_000));
    }

    public void help(Player player) {
        messages.info(player, "/gzscatter <prefabs> [radius] [options] binds a scatter brush. Prefabs:");
        list(player);
        messages.info(player, "Separate several with commas (repeat one to make it more common), or name a folder "
                + "ending in / for all its schematics. Options: %s", String.join(" ", ScatterOptions.NAMES));
    }

    public void list(Player player) {
        List<String> names = library.get().names(player.getUniqueId());
        Component line = Component.text("  ");
        int shown = 0;
        for (String name : names) {
            if (shown++ >= 60) {
                line = line.append(Component.text("… and " + (names.size() - 60) + " more", NamedTextColor.GRAY));
                break;
            }
            boolean builtIn = ProceduralPrefab.names().contains(name);
            line = line.append(Component.text(name, builtIn ? NamedTextColor.GREEN : NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.suggestCommand("/gzscatter " + name + " 8"))).append(Component.text("  "));
        }
        player.sendMessage(line);
        if (names.size() == ProceduralPrefab.names().size()) {
            messages.info(player, "No schematics yet: save some with //copy and //schem save <name>.");
        }
    }

    public void bind(Player player, String input) {
        List<String> tokens = new ArrayList<>(Arrays.asList(input.trim().split("\\s+")));
        if (tokens.isEmpty() || tokens.get(0).isEmpty()) {
            help(player);
            return;
        }
        String spec = tokens.remove(0);
        int radius = 8;
        if (!tokens.isEmpty() && tokens.get(0).matches("\\d+")) {
            radius = Integer.parseInt(tokens.remove(0));
        }
        if (radius < 1 || radius > maxRadius()) {
            messages.error(player, "Radius must be between 1 and %d (scatter.max-radius).", maxRadius());
            return;
        }
        ScatterOptions options;
        List<Prefab> prefabs;
        try {
            options = ScatterOptions.parse(tokens);
            prefabs = library.get().resolve(player.getUniqueId(), spec);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        UUID owner = player.getUniqueId();
        ScatterBrush brush = new ScatterBrush(prefabs, options, rules, maxBlocks(),
                ThreadLocalRandom.current().nextLong(), (placed, blocks) -> {
            Player online = plugin.getServer().getPlayer(owner);
            if (online != null) {
                online.sendActionBar(Component.text(placed == 0 ? "No room here: steep, crowded, roofs or water"
                        : "Placed " + placed + " prefab" + (placed == 1 ? "" : "s") + " (" + blocks + " blocks)",
                        placed == 0 ? NamedTextColor.RED : NamedTextColor.GRAY));
            }
        });
        try {
            FaweBrushBinder.bind(player, brush, PERMISSION, radius, "gzmn scatter " + spec, null);
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        prefabs.forEach(p -> counts.merge(p.name(), 1, Integer::sum));
        List<String> named = new ArrayList<>();
        counts.forEach((name, n) -> named.add(n > 1 ? name + " x" + n : name));
        messages.success(player, "Scatter brush bound (radius %d): %s.", radius, String.join(", ", named));
        messages.info(player, "%s. Each click places a new random layout; /gzhover shows the next one first.",
                options.describe());
    }

    public List<String> suggestions(Player player, String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (prefix.isEmpty()) {
            String head = current.contains(",") ? current.substring(0, current.lastIndexOf(',') + 1) : "";
            String last = current.substring(head.length());
            for (String name : library.get().names(player.getUniqueId())) {
                if (name.toLowerCase(Locale.ROOT).startsWith(last)) {
                    out.add(head + name);
                }
            }
            if (!current.isEmpty() && !current.endsWith(",")) {
                out.add(current + ",");
            }
            return out;
        }
        if (current.startsWith("on=")) {
            List.of("on=terrain", "on=any").stream().filter(v -> v.startsWith(current)).forEach(v -> out.add(prefix + v));
            return out;
        }
        if (current.startsWith("replace=")) {
            List.of("replace=keep", "replace=all").stream().filter(v -> v.startsWith(current))
                    .forEach(v -> out.add(prefix + v));
            return out;
        }
        for (String option : ScatterOptions.NAMES) {
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
