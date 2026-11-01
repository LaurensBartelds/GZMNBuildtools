package nl.gzmn.gZMNBuildtools.shared.preview;

import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows an edit to one player as client-side "ghost" blocks before it is
 * applied. The real world is untouched until the player confirms; cancelling,
 * timing out, leaving or changing world restores what the player sees.
 */
public final class PreviewService implements Listener {

    private record Pending(String world, Map<Position, BlockData> shown, Runnable apply, BukkitTask timeout) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PreviewService(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public int maxBlocks() {
        return Math.max(1, plugin.getConfig().getInt("preview.max-blocks", 60_000));
    }

    private int seconds() {
        return Math.max(5, plugin.getConfig().getInt("preview.seconds", 60));
    }

    /**
     * Shows {@code changes} to the player; call on the main thread.
     *
     * @param apply performs the real edit when the player confirms
     */
    public void show(Player player, Map<Position, BlockData> changes, String label, Runnable apply) {
        cancel(player, false);
        if (changes.isEmpty()) {
            messages.info(player, "Nothing would change.");
            return;
        }
        if (changes.size() > maxBlocks()) {
            messages.error(player, "That preview has %d blocks, above the limit of %d (preview.max-blocks).",
                    changes.size(), maxBlocks());
            return;
        }
        player.sendMultiBlockChange(changes);
        UUID id = player.getUniqueId();
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online != null && pending.containsKey(id)) {
                cancel(online, false);
                messages.info(online, "Preview expired.");
            }
        }, seconds() * 20L);
        pending.put(id, new Pending(player.getWorld().getName(), new HashMap<>(changes), apply, timeout));
        messages.info(player, "Previewing %s: %d blocks (only you can see this).", label, changes.size());
        player.sendMessage(Component.text("[Apply]", NamedTextColor.GREEN)
                .hoverEvent(HoverEvent.showText(Component.text("Place exactly what you see")))
                .clickEvent(ClickEvent.runCommand("/gzpreview apply"))
                .append(Component.text("  "))
                .append(Component.text("[Cancel]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/gzpreview cancel")))
                .append(Component.text("  expires in " + seconds() + "s", NamedTextColor.DARK_GRAY)));
    }

    /** The positions the player's pending preview shows (empty when there is none). */
    public Set<Position> shown(Player player) {
        Pending p = pending.get(player.getUniqueId());
        return p == null ? Set.of() : Set.copyOf(p.shown().keySet());
    }

    public boolean has(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void apply(Player player) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) {
            messages.error(player, "You have no preview to apply.");
            return;
        }
        p.timeout.cancel();
        // The real edit replaces the ghost blocks; no need to restore them first.
        p.apply.run();
    }

    public void cancel(Player player) {
        if (!cancel(player, true)) {
            messages.error(player, "You have no preview to cancel.");
        }
    }

    private boolean cancel(Player player, boolean tell) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) {
            return false;
        }
        p.timeout.cancel();
        World world = Bukkit.getWorld(p.world);
        if (world != null && player.getWorld().equals(world)) {
            Map<Position, BlockData> real = new HashMap<>();
            for (Position pos : p.shown.keySet()) {
                real.put(pos, world.getBlockAt(pos.blockX(), pos.blockY(), pos.blockZ()).getBlockData());
            }
            player.sendMultiBlockChange(real);
        }
        if (tell) {
            messages.info(player, "Preview cancelled.");
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p != null) {
            p.timeout.cancel();
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p != null) {
            p.timeout.cancel();
        }
    }
}
