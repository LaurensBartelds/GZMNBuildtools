package nl.gzmn.gZMNBuildtools.modules.hover.live;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.core.PlayerDataStore;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Shows, as ghost blocks only the player can see, what a click with the held
 * brush would change at the block they are looking at. Recomputed when the
 * target, brush, size, mask or brush settings change, at most one computation
 * per player at a time.
 */
public final class HoverService implements Listener {

    private static final String DATA = "hover-preview";
    /** After a click, give the real edit time to land before previewing again. */
    private static final long QUIET_AFTER_CLICK_MS = 600;

    /** Per-player hover state; only touched on the main thread. */
    private static final class State {
        boolean enabled;
        List<Object> key;
        Map<Position, BlockData> shown = Map.of();
        boolean busy;
        long quietUntil;
        int generation;
    }

    /** What the player is aiming the brush at. */
    private record Target(com.sk89q.worldedit.entity.Player actor, LocalSession session, PreviewableBrush brush,
                          BlockVector3 position, double size, Mask mask, Mask global, List<Object> key) {
    }

    private final JavaPlugin plugin;
    private final PreviewService previews;
    private final PlayerDataStore data;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();
    private BukkitTask task;

    public HoverService(JavaPlugin plugin, PreviewService previews, PlayerDataStore data) {
        this.plugin = plugin;
        this.previews = previews;
        this.data = data;
    }

    private int maxBlocks() {
        return Math.max(1, plugin.getConfig().getInt("hover.max-blocks", 4000));
    }

    private int range() {
        return Math.max(4, plugin.getConfig().getInt("hover.range", 64));
    }

    public void start() {
        long interval = Math.max(1, plugin.getConfig().getInt("hover.interval-ticks", 4));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            State state = states.get(player.getUniqueId());
            if (state != null) {
                clear(player, state);
            }
        }
        states.clear();
    }

    private State state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> {
            State s = new State();
            Object saved = data.get(id, DATA, "enabled");
            s.enabled = saved instanceof Boolean b ? b : plugin.getConfig().getBoolean("hover.enabled-by-default", false);
            return s;
        });
    }

    public boolean enabled(Player player) {
        return state(player).enabled;
    }

    public void setEnabled(Player player, boolean on) {
        State state = state(player);
        state.enabled = on;
        data.set(player.getUniqueId(), DATA, "enabled", on);
        if (!on) {
            clear(player, state);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            State state = state(player);
            if (!state.enabled || state.busy || now < state.quietUntil) {
                continue;
            }
            if (previews.has(player)) {
                // An explicit preview is showing: step aside without touching its ghost blocks.
                if (state.key != null || !state.shown.isEmpty()) {
                    restore(player, state, previews.shown(player));
                }
                continue;
            }
            Target target;
            try {
                target = target(player);
            } catch (RuntimeException e) {
                target = null;
            }
            if (target == null) {
                if (state.key != null || !state.shown.isEmpty()) {
                    clear(player, state);
                }
                continue;
            }
            if (!target.key().equals(state.key)) {
                compute(player, state, target);
            }
        }
    }

    private Target target(Player player) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(actor);
        if (session == null) {
            return null;
        }
        Tool tool = session.getTool(actor);
        if (!(tool instanceof BrushTool brushTool)) {
            return null;
        }
        BrushSettings settings = brushTool.getContext();
        if (!(settings.getBrush() instanceof PreviewableBrush brush) || !settings.canUse(actor)) {
            return null;
        }
        Block block = player.getTargetBlockExact(Math.min(brushTool.getRange(), range()));
        if (block == null || block.getType().isAir()) {
            return null;
        }
        double size = settings.getSize();
        Mask mask = settings.getMask();
        Mask global = session.getMask();
        List<Object> key = Arrays.asList(player.getWorld().getName(), block.getX(), block.getY(),
                block.getZ(), size, System.identityHashCode(brush), System.identityHashCode(mask),
                System.identityHashCode(global), brush.previewState());
        return new Target(actor, session, brush, BlockVector3.at(block.getX(), block.getY(), block.getZ()), size,
                mask, global, key);
    }

    private void compute(Player player, State state, Target target) {
        state.busy = true;
        state.key = target.key();
        int generation = ++state.generation;
        int cap = maxBlocks();
        Fawe.instance().getQueueHandler().async(() -> {
            Map<BlockVector3, BlockState> changes = new HashMap<>();
            int[] count = {0};
            try (EditSession editSession = target.session().createEditSession(target.actor())) {
                target.brush().preview(editSession, target.position(), target.size(), (x, y, z, block) -> {
                    BlockVector3 at = BlockVector3.at(x, y, z);
                    if ((target.mask() != null && !target.mask().test(at))
                            || (target.global() != null && !target.global().test(at))) {
                        return;
                    }
                    if (++count[0] <= cap) {
                        changes.put(at, block);
                    }
                });
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.FINE, "Hover preview failed for " + player.getName(), e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                state.busy = false;
                if (generation != state.generation || !state.enabled || !player.isOnline()) {
                    return;
                }
                if (count[0] > cap) {
                    show(player, state, Map.of());
                    player.sendActionBar(Component.text("Hover preview: this stroke changes " + count[0]
                            + " blocks, more than " + cap + " (hover.max-blocks)", NamedTextColor.GRAY));
                    return;
                }
                Map<Position, BlockData> ghost = new HashMap<>();
                changes.forEach((pos, block) -> ghost.put(Position.block(pos.x(), pos.y(), pos.z()),
                        BukkitAdapter.adapt(block)));
                show(player, state, ghost);
            });
        });
    }

    /** Replaces the shown ghosts with {@code ghost}, restoring the real blocks that are no longer covered. */
    private void show(Player player, State state, Map<Position, BlockData> ghost) {
        Map<Position, BlockData> send = new HashMap<>();
        World world = player.getWorld();
        for (Position old : state.shown.keySet()) {
            if (!ghost.containsKey(old)) {
                send.put(old, world.getBlockAt(old.blockX(), old.blockY(), old.blockZ()).getBlockData());
            }
        }
        send.putAll(ghost);
        if (!send.isEmpty()) {
            player.sendMultiBlockChange(send);
        }
        state.shown = ghost;
    }

    private void restore(Player player, State state, Set<Position> keep) {
        if (!state.shown.isEmpty()) {
            Map<Position, BlockData> send = new HashMap<>();
            World world = player.getWorld();
            for (Position pos : state.shown.keySet()) {
                if (!keep.contains(pos)) {
                    send.put(pos, world.getBlockAt(pos.blockX(), pos.blockY(), pos.blockZ()).getBlockData());
                }
            }
            if (!send.isEmpty()) {
                player.sendMultiBlockChange(send);
            }
        }
        state.shown = Map.of();
        state.key = null;
        state.generation++;
    }

    private void clear(Player player, State state) {
        restore(player, state, Set.of());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        State state = states.get(event.getPlayer().getUniqueId());
        if (state != null && state.enabled && !state.shown.isEmpty()) {
            // Probably a brush stroke: keep the ghosts (they match what is being placed) and look again afterwards.
            state.key = null;
            state.generation++;
            state.quietUntil = System.currentTimeMillis() + QUIET_AFTER_CLICK_MS;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        State state = states.get(event.getPlayer().getUniqueId());
        if (state != null) {
            state.shown = Map.of();
            state.key = null;
            state.generation++;
        }
    }
}
