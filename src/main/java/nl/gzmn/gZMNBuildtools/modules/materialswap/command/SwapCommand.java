package nl.gzmn.gZMNBuildtools.modules.materialswap.command;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.materialswap.engine.MaterialSwap;
import nl.gzmn.gZMNBuildtools.modules.materialswap.engine.SwapOptions;
import nl.gzmn.gZMNBuildtools.modules.materialswap.fawe.MaterialSwapBrush;
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

/** {@code /gzswap}: swap a build's material, as a brush, for the whole build, or as a preview. */
public class SwapCommand {

    public static final String PERMISSION = "gzmnbuildtools.swap";
    private static final int RANGE = 200;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BuildEditor editor;
    private final MaterialSwap swap;
    private final PreviewService previews;

    private final Limits limits;

    public SwapCommand(JavaPlugin plugin, Messages messages, BuildEditor editor, PreviewService previews,
                       Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.editor = editor;
        this.swap = new MaterialSwap(editor.blocks().catalog(), editor.blocks().variants());
        this.previews = previews;
        this.limits = limits;
    }

    private int maxRadius(Player player) {
        return limits.get(player, Limits.Kind.RADIUS,
                Math.max(1, plugin.getConfig().getInt("smart-gradient.max-radius", 32)));
    }

    private Optional<SwapOptions> parse(Player player, String input) {
        try {
            return Optional.of(SwapOptions.parse(input, editor.blocks().catalog()::exists, maxRadius(player), 4));
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return Optional.empty();
        }
    }

    public void help(Player player) {
        messages.info(player, "Material swap keeps stairs, slabs, walls and mossy/cracked forms:");
        messages.info(player, "  /gzswap %s  binds the brush", SwapOptions.USAGE);
        messages.info(player, "  /gzswap paint <to-block> ...  swaps the whole build you look at");
        messages.info(player, "  /gzswap preview <to-block> ...  shows it as ghost blocks first");
    }

    public void bind(Player player, String input) {
        Optional<SwapOptions> options = parse(player, input);
        if (options.isEmpty()) {
            return;
        }
        UUID owner = player.getUniqueId();
        try {
            FaweBrushBinder.bind(player, new MaterialSwapBrush(editor, swap, options.get(), o -> feedback(owner, o)),
                    PERMISSION, options.get().radius(), maxRadius(player), "gzmn swap " + input.trim(), null);
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        }
        messages.success(player, "Swap brush bound: %s. Click a build to swap it.", options.get().describe());
    }

    private void feedback(UUID owner, BuildEditor.Outcome outcome) {
        Player player = plugin.getServer().getPlayer(owner);
        if (player == null) {
            return;
        }
        player.sendActionBar(outcome.ok()
                ? Component.text(outcome.painted() + " swapped · " + outcome.scan().bounds() + " build",
                NamedTextColor.GRAY)
                : Component.text(outcome.problem(), NamedTextColor.RED));
    }

    public void paint(Player player, String input, boolean preview) {
        Optional<SwapOptions> options = parse(player, input);
        Optional<BlockVector3> target = PlayerEdits.lookedAt(player, RANGE);
        if (options.isEmpty()) {
            return;
        }
        if (target.isEmpty()) {
            messages.error(player, "Look at a block of the build first.");
            return;
        }
        SwapOptions o = options.get();
        Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
        PlayerEdits.async(player, editSession -> {
            BuildEditor.StateSink sink = preview
                    ? (x, y, z, state) -> changes.put(BlockVector3.at(x, y, z), state)
                    : BuildEditor.StateSink.into(editSession);
            BuildEditor.Outcome outcome = editor.run(editSession, target.get(), swap.mode(o),
                    List.of(swap.targetMaterial(o)), null, (scan, source) -> swap.chooser(o), sink);
            if (!outcome.ok()) {
                messages.error(player, "%s", outcome.problem());
                return false;
            }
            if (preview) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes),
                        "the swap to " + o.to(), () -> apply(player, changes)));
                return false;
            }
            messages.success(player, "Swapped %d blocks of a %s build to %s. //undo reverts it.", outcome.painted(),
                    outcome.scan().bounds().toString(), o.to());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Material swap failed for " + player.getName(), e);
            messages.error(player, "Swapping failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }

    /** Suggestions for "<to-block> [key=value ...]". */
    public List<String> suggestions(String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (current.startsWith("from=")) {
            editor.blocks().generator().candidates().stream().filter(id -> ("from=" + id).startsWith(current))
                    .limit(30).forEach(id -> out.add(prefix + "from=" + id));
            return out;
        }
        if (current.startsWith("shapes=") || current.startsWith("textures=")) {
            String key = current.substring(0, current.indexOf('=') + 1);
            List<String> values = key.equals("shapes=") ? List.of("keep", "full") : List.of("keep", "plain");
            values.stream().map(v -> key + v).filter(v -> v.startsWith(current)).forEach(v -> out.add(prefix + v));
            return out;
        }
        if (prefix.isEmpty()) {
            editor.blocks().generator().candidates().stream().filter(id -> id.startsWith(current)).limit(40)
                    .forEach(out::add);
            return out;
        }
        for (String option : List.of("from=", "radius=", "shapes=", "textures=")) {
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
