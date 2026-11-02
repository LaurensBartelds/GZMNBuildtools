package nl.gzmn.gZMNBuildtools.modules.weathering.command;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.weathering.engine.WeatherOptions;
import nl.gzmn.gZMNBuildtools.modules.weathering.fawe.WeatheringBrush;
import nl.gzmn.gZMNBuildtools.modules.weathering.fawe.WeatheringRunner;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.FaweBrushBinder;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** {@code /weathering}: age a build with moss, cracks, worn edges and vines. */
public class WeatheringCommand {

    public static final String PERMISSION = "gzmnbuildtools.weathering";
    private static final int RANGE = 200;
    private static final List<String> OPTIONS = List.of("strength=", "moss=", "cracks=", "decay=", "vines=", "radius=",
            "target=");

    private final JavaPlugin plugin;
    private final Messages messages;
    private final WeatheringRunner runner;
    private final PreviewService previews;
    private final Limits limits;

    public WeatheringCommand(JavaPlugin plugin, Messages messages, BuildEditor editor, PreviewService previews,
                             Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.runner = new WeatheringRunner(editor);
        this.previews = previews;
        this.limits = limits;
    }

    private int maxRadius(Player player) {
        return limits.get(player, Limits.Kind.RADIUS,
                Math.max(1, plugin.getConfig().getInt("smart-gradient.max-radius", 32)));
    }

    private Optional<WeatherOptions> parse(Player player, String input) {
        try {
            return Optional.of(WeatherOptions.parse(input, maxRadius(player)));
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return Optional.empty();
        }
    }

    public void bind(Player player, String input) {
        Optional<WeatherOptions> options = parse(player, input);
        if (options.isEmpty()) {
            return;
        }
        UUID owner = player.getUniqueId();
        try {
            FaweBrushBinder.bind(player, new WeatheringBrush(runner, options.get(), r -> feedback(owner, r)),
                    PERMISSION, options.get().radius(), maxRadius(player), "gzmn weathering " + input.trim(), null);
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        }
        messages.success(player, "Weathering brush bound (%s). Click a build to age it; click again for more.",
                options.get().describe());
        messages.info(player, "Also: /weathering paint [options] for a whole build, /weathering preview to look first.");
    }

    private void feedback(UUID owner, WeatheringRunner.Result result) {
        Player player = plugin.getServer().getPlayer(owner);
        if (player == null) {
            return;
        }
        BuildEditor.Outcome o = result.outcome();
        player.sendActionBar(o.ok()
                ? Component.text(o.painted() + " weathered, " + result.vines() + " vines · " + o.scan().bounds()
                + " build", NamedTextColor.GRAY)
                : Component.text(o.problem(), NamedTextColor.RED));
    }

    public void paint(Player player, String input, boolean preview) {
        Optional<WeatherOptions> options = parse(player, input);
        if (options.isEmpty()) {
            return;
        }
        Optional<BlockVector3> target = PlayerEdits.lookedAt(player, RANGE);
        if (target.isEmpty()) {
            messages.error(player, "Look at a block of the build first.");
            return;
        }
        Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
        PlayerEdits.async(player, editSession -> {
            BuildEditor.StateSink sink = preview
                    ? (x, y, z, state) -> changes.put(BlockVector3.at(x, y, z), state)
                    : BuildEditor.StateSink.into(editSession);
            WeatheringRunner.Result result = runner.run(editSession, target.get(), options.get(), null, sink);
            BuildEditor.Outcome o = result.outcome();
            if (!o.ok()) {
                messages.error(player, "%s", o.problem());
                return false;
            }
            if (preview) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes),
                        "weathering on this " + o.scan().bounds() + " build", () -> apply(player, changes)));
                return false;
            }
            messages.success(player, "Weathered a %s build: %d blocks changed, %d vines. //undo reverts it.",
                    o.scan().bounds().toString(), o.painted(), result.vines());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Weathering failed for " + player.getName(), e);
            messages.error(player, "Weathering failed: %s", e.getMessage());
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
        List<String> out = new ArrayList<>();
        if (current.startsWith("target=")) {
            List.of("target=build", "target=material").stream().filter(v -> v.startsWith(current))
                    .forEach(v -> out.add(prefix + v));
            return out;
        }
        for (String option : OPTIONS) {
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
