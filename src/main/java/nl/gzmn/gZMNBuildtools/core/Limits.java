package nl.gzmn.gZMNBuildtools.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Per-rank limits. Ranks are defined under {@code limits.ranks} in config.yml
 * and given with the permission {@code gzmnbuildtools.limits.<rank>}; the
 * {@code default} rank applies to everyone. A player gets the highest value
 * among their ranks, and where no rank sets a limit, the module's own setting.
 *
 * <p>Permissions are only checked on the main thread; the result is cached per
 * player (refreshed on join and every few seconds), so brushes running on
 * FAWE's threads and the web editor can read limits safely.</p>
 */
public final class Limits implements Listener {

    /** What can be limited. */
    public enum Kind {
        /** Brush radius. */
        RADIUS("radius", "brush radius"),
        /** Blocks one generator, spline or scatter click may change. */
        BLOCKS("blocks", "blocks per action"),
        /** Blocks in a selection a command works on. */
        SELECTION("selection", "selection size"),
        /** Longest side of a pixel-art picture. */
        IMAGE("image", "picture size"),
        /** Saved loadouts per brush. */
        LOADOUTS("loadouts", "saved loadouts");

        private final String key;
        private final String label;

        Kind(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String key() {
            return key;
        }

        public String label() {
            return label;
        }
    }

    public static final String RANK_PERMISSION = "gzmnbuildtools.limits.";
    public static final String UNLIMITED_PERMISSION = "gzmnbuildtools.limits.unlimited";
    public static final String DEFAULT_RANK = "default";
    /** Used for "no limit"; small enough that callers can still do arithmetic with it. */
    public static final int UNLIMITED = 1_000_000_000;

    /** A player's limits as worked out from their ranks. */
    public record Resolved(boolean unlimited, Map<Kind, Integer> values, Map<Kind, String> sources,
                           List<String> ranks) {

        public int get(Kind kind, int fallback) {
            if (unlimited) {
                return UNLIMITED;
            }
            Integer value = values.get(kind);
            return value == null ? fallback : value;
        }

        /** Which rank decided the limit, or null when it is the module's own setting. */
        public String source(Kind kind) {
            return unlimited ? "unlimited" : sources.get(kind);
        }
    }

    private volatile Map<String, Map<Kind, Integer>> ranks = Map.of();
    private volatile Resolved defaults = new Resolved(false, Map.of(), Map.of(), List.of());
    private final Map<UUID, Resolved> resolved = new ConcurrentHashMap<>();
    private BukkitTask refresher;

    /** Reads {@code limits:} from the config; problems are returned as messages. */
    public List<String> reload(ConfigurationSection config) {
        List<String> problems = new ArrayList<>();
        Map<String, Map<Kind, Integer>> parsed = new LinkedHashMap<>();
        ConfigurationSection limits = config.getConfigurationSection("limits");
        if (limits != null) {
            parse(DEFAULT_RANK, limits.getConfigurationSection(DEFAULT_RANK), parsed, problems);
            ConfigurationSection rankSection = limits.getConfigurationSection("ranks");
            if (rankSection != null) {
                for (String rank : rankSection.getKeys(false)) {
                    String name = rank.toLowerCase(Locale.ROOT);
                    if (name.equals("unlimited") || name.equals(DEFAULT_RANK)) {
                        problems.add("limits.ranks." + rank + ": that name is reserved");
                        continue;
                    }
                    parse(name, rankSection.getConfigurationSection(rank), parsed, problems);
                }
            }
        }
        ranks = Collections.unmodifiableMap(parsed);
        defaults = resolve(permission -> false);
        resolved.clear();
        if (Bukkit.getServer() != null && Bukkit.isPrimaryThread()) {
            Bukkit.getOnlinePlayers().forEach(this::refresh);
        }
        return problems;
    }

    private static void parse(String rank, ConfigurationSection section, Map<String, Map<Kind, Integer>> out,
                              List<String> problems) {
        if (section == null) {
            return;
        }
        Map<Kind, Integer> values = new EnumMap<>(Kind.class);
        for (String key : section.getKeys(false)) {
            Kind kind = null;
            for (Kind k : Kind.values()) {
                if (k.key().equalsIgnoreCase(key)) {
                    kind = k;
                }
            }
            if (kind == null) {
                problems.add("limits " + rank + "." + key + ": unknown limit (use radius, blocks, selection, "
                        + "image or loadouts)");
                continue;
            }
            if (!section.isInt(key) || section.getInt(key) < 0) {
                problems.add("limits " + rank + "." + key + ": must be a whole number of 0 or more");
                continue;
            }
            values.put(kind, section.getInt(key));
        }
        out.put(rank, values);
    }

    /** Works out a player's limits from a permission check. */
    public Resolved resolve(Predicate<String> hasPermission) {
        if (hasPermission.test(UNLIMITED_PERMISSION)) {
            return new Resolved(true, Map.of(), Map.of(), List.of("unlimited"));
        }
        Map<Kind, Integer> values = new EnumMap<>(Kind.class);
        Map<Kind, String> sources = new EnumMap<>(Kind.class);
        List<String> held = new ArrayList<>();
        for (Map.Entry<String, Map<Kind, Integer>> rank : ranks.entrySet()) {
            boolean applies = rank.getKey().equals(DEFAULT_RANK) || hasPermission.test(RANK_PERMISSION + rank.getKey());
            if (!applies) {
                continue;
            }
            held.add(rank.getKey());
            rank.getValue().forEach((kind, value) -> {
                Integer current = values.get(kind);
                if (current == null || value > current) {
                    values.put(kind, value);
                    sources.put(kind, rank.getKey());
                }
            });
        }
        return new Resolved(false, Map.copyOf(values), Map.copyOf(sources), List.copyOf(held));
    }

    /** Re-checks a player's ranks; main thread only. */
    public Resolved refresh(Player player) {
        Resolved r = resolve(player::hasPermission);
        resolved.put(player.getUniqueId(), r);
        return r;
    }

    /** The player's limits; refreshed first when called on the main thread. */
    public Resolved of(Player player) {
        if (Bukkit.isPrimaryThread()) {
            return refresh(player);
        }
        return of(player.getUniqueId());
    }

    /** The cached limits of a player (the default rank for players not seen yet). Safe from any thread. */
    public Resolved of(UUID player) {
        Resolved r = resolved.get(player);
        return r == null ? defaults : r;
    }

    public int get(Player player, Kind kind, int fallback) {
        return of(player).get(kind, fallback);
    }

    public int get(UUID player, Kind kind, int fallback) {
        return of(player).get(kind, fallback);
    }

    public List<String> rankNames() {
        return List.copyOf(ranks.keySet());
    }

    public void start(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        refresher = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::refresh), 100L, 100L);
    }

    public void stop() {
        if (refresher != null) {
            refresher.cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resolved.remove(event.getPlayer().getUniqueId());
    }
}
