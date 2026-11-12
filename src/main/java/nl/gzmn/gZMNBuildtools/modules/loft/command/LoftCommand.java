package nl.gzmn.gZMNBuildtools.modules.loft.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftOptions;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftPlanner;
import nl.gzmn.gZMNBuildtools.modules.loft.engine.LoftSpec;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.shapes.Splines;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PartPlacer;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** {@code /gzloft}: pick points with frames, then loft a smooth surface through them. */
public class LoftCommand {

    public static final String PERMISSION = "gzmnbuildtools.loft";

    private static final double DEFAULT_FRAME = 3;
    private static final double MAX_FRAME = 32;

    private record Points(String world, List<LoftPlanner.Station> list) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final Limits limits;
    private final Map<UUID, Points> points = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> showing = new ConcurrentHashMap<>();

    public LoftCommand(JavaPlugin plugin, Messages messages, PreviewService previews, Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.previews = previews;
        this.limits = limits;
    }

    private int maxBlocks(Player player) {
        return limits.get(player, Limits.Kind.BLOCKS,
                Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000)));
    }

    private List<LoftPlanner.Station> pointsOf(Player player) {
        Points p = points.get(player.getUniqueId());
        if (p == null || !p.world.equals(player.getWorld().getName())) {
            p = new Points(player.getWorld().getName(), new ArrayList<>());
            points.put(player.getUniqueId(), p);
        }
        return p.list;
    }

    public void help(Player player) {
        messages.info(player, "Loft: %d point(s). Look at a block and /gzloft add [w] [h] (or /gzloft here).",
                pointsOf(player).size());
        messages.info(player, "Usage: %s", LoftSpec.INSTANCE.usage());
        messages.info(player, "Also: frame, remove, clear, list, show. Add -p to a build to preview it.");
    }

    public void add(Player player, boolean atFeet, String rawSize) {
        double w = DEFAULT_FRAME;
        double h = DEFAULT_FRAME;
        if (rawSize != null && !rawSize.isBlank()) {
            String[] parts = rawSize.trim().split("\\s+");
            try {
                if (parts.length > 2) {
                    throw new NumberFormatException();
                }
                w = Double.parseDouble(parts[0]);
                h = parts.length > 1 ? Double.parseDouble(parts[1]) : w;
            } catch (NumberFormatException e) {
                messages.error(player, "Frame size must be one or two numbers between 1 and %d.", (int) MAX_FRAME);
                return;
            }
            if (w < 1 || h < 1 || w > MAX_FRAME || h > MAX_FRAME) {
                messages.error(player, "Frame size must be one or two numbers between 1 and %d.", (int) MAX_FRAME);
                return;
            }
        }
        Block block = atFeet ? player.getLocation().getBlock().getRelative(0, -1, 0)
                : player.getTargetBlockExact(200);
        if (block == null || block.getType().isAir()) {
            messages.error(player, "Look at a block to add it as a loft point (or use /gzloft here).");
            return;
        }
        List<LoftPlanner.Station> list = pointsOf(player);
        list.add(new LoftPlanner.Station(block.getX(), block.getY(), block.getZ(), w, h));
        player.spawnParticle(Particle.FLAME, block.getX() + 0.5, block.getY() + 1.1, block.getZ() + 0.5, 12, 0.2, 0.2,
                0.2, 0.01);
        messages.success(player, "Point %d at %d, %d, %d (frame %gx%g).", list.size(), block.getX(), block.getY(),
                block.getZ(), w, h);
        if (list.size() >= 2) {
            show(player, 3);
        }
    }

    public void frame(Player player, String rawSize) {
        List<LoftPlanner.Station> list = pointsOf(player);
        if (list.isEmpty()) {
            messages.error(player, "No loft point to set the frame of. Add a point first.");
            return;
        }
        String[] parts = rawSize == null ? new String[0] : rawSize.trim().split("\\s+");
        double w;
        double h;
        try {
            if (parts.length < 1 || parts.length > 2) {
                throw new NumberFormatException();
            }
            w = Double.parseDouble(parts[0]);
            h = parts.length > 1 ? Double.parseDouble(parts[1]) : w;
            if (w < 1 || h < 1 || w > MAX_FRAME || h > MAX_FRAME) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            messages.error(player, "Frame size must be one or two numbers between 1 and %d.", (int) MAX_FRAME);
            return;
        }
        LoftPlanner.Station last = list.get(list.size() - 1);
        list.set(list.size() - 1, new LoftPlanner.Station(last.x(), last.y(), last.z(), w, h));
        messages.success(player, "Frame of the last point is now %gx%g.", w, h);
    }

    public void remove(Player player) {
        List<LoftPlanner.Station> list = pointsOf(player);
        if (list.isEmpty()) {
            messages.error(player, "No loft points to remove.");
            return;
        }
        LoftPlanner.Station removed = list.remove(list.size() - 1);
        messages.success(player, "Removed point %d, %d, %d (%d left).", (int) removed.x(), (int) removed.y(),
                (int) removed.z(), list.size());
    }

    public void clear(Player player) {
        pointsOf(player).clear();
        messages.success(player, "Loft points cleared.");
    }

    public void list(Player player) {
        List<LoftPlanner.Station> list = pointsOf(player);
        if (list.isEmpty()) {
            messages.info(player, "No loft points yet.");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            LoftPlanner.Station s = list.get(i);
            messages.info(player, "  %d: %d, %d, %d (%gx%g)", i + 1, (int) s.x(), (int) s.y(), (int) s.z(),
                    s.width(), s.height());
        }
    }

    public void show(Player player, int seconds) {
        List<LoftPlanner.Station> list = new ArrayList<>(pointsOf(player));
        if (list.size() < 2) {
            messages.error(player, "Add at least two points first.");
            return;
        }
        List<int[]> ints = new ArrayList<>();
        for (LoftPlanner.Station s : list) {
            ints.add(new int[]{(int) Math.round(s.x()), (int) Math.round(s.y()), (int) Math.round(s.z())});
        }
        List<double[]> samples = Splines.sample(ints, 1.5);
        BukkitTask old = showing.remove(player.getUniqueId());
        if (old != null) {
            old.cancel();
        }
        int[] runs = {seconds * 2};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || runs[0]-- <= 0) {
                BukkitTask self = showing.remove(player.getUniqueId());
                if (self != null) {
                    self.cancel();
                }
                return;
            }
            for (double[] s : samples) {
                player.spawnParticle(Particle.END_ROD, s[0] + 0.5, s[1] + 1.2, s[2] + 0.5, 1, 0, 0, 0, 0);
            }
            for (LoftPlanner.Station p : list) {
                player.spawnParticle(Particle.FLAME, p.x() + 0.5, p.y() + 1.2, p.z() + 0.5, 2, 0.1, 0.1, 0.1, 0);
            }
        }, 0L, 10L);
        showing.put(player.getUniqueId(), task);
    }

    public void build(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        String rest = String.join(" ", tokens);
        if (rest.isBlank()) {
            messages.error(player, "Usage: %s", LoftSpec.INSTANCE.usage());
            return;
        }
        BrushArguments<LoftSpec> args;
        try {
            args = BrushArguments.parse(LoftSpec.INSTANCE, rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        List<LoftPlanner.Station> stations = new ArrayList<>(pointsOf(player));
        if (stations.size() < 2) {
            messages.error(player, "Add at least two loft points first (/gzloft add while looking at a block).");
            return;
        }
        LoftOptions options = new LoftOptions(args.get("outline") >= 0.5, args.get("fill") >= 0.5,
                args.get("smooth") >= 0.5, args.get("lowpoly") >= 0.5, args.get("stack-down") >= 0.5,
                args.get("density"), 42L);

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        PartPlacer placer;
        try {
            placer = new PartPlacer(List.of(parsePattern(actor, session, args.pattern())));
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        }

        int[] count = {0};
        LoftPlanner.plan(stations, options, (x, z) -> -1).cells().forEach(c -> count[0]++);
        if (count[0] > maxBlocks(player)) {
            messages.error(player, "That loft has %d blocks, above your limit of %d.", count[0], maxBlocks(player));
            return;
        }

        int minY = player.getWorld().getMinHeight();
        int maxY = player.getWorld().getMaxHeight() - 1;
        String label = "a loft through " + stations.size() + " points";
        PlayerEdits.async(player, editSession -> {
            LoftPlanner.ColumnGround ground = (x, z) -> {
                for (int y = maxY; y >= minY; y--) {
                    if (editSession.getBlock(x, y, z).getBlockType().getMaterial().isAir()) {
                        continue;
                    }
                    return y;
                }
                return -1;
            };
            LoftPlanner.Plan plan = LoftPlanner.plan(stations, options, ground);
            if (plan.problem() != null) {
                messages.error(player, "%s", plan.problem());
                return false;
            }
            if (plan.blockCount() > maxBlocks(player)) {
                messages.error(player, "That loft has %d blocks, above your limit of %d.", plan.blockCount(),
                        maxBlocks(player));
                return false;
            }
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (LoftPlanner.Cell cell : plan.cells()) {
                if (cell.y() >= minY && cell.y() <= maxY) {
                    BlockState state = placer.state(cell.x(), cell.y(), cell.z(), 0, (char) 0);
                    if (state != null) {
                        changes.put(BlockVector3.at(cell.x(), cell.y(), cell.z()), state);
                    }
                }
            }
            if (preview) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes), label,
                        () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Lofted %d blocks through %d points. //undo reverts it.", changes.size(),
                    stations.size());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Loft build failed for " + player.getName(), e);
            messages.error(player, "Loft failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the loft: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
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

    public List<String> suggestions(String typed) {
        if (!typed.contains("=") && !typed.contains(" ")) {
            String last = typed.toLowerCase(Locale.ROOT);
            return List.of("oak_planks", "stone_bricks", "white_wool", "glass", "#stripes[y][1][stone][andesite]")
                    .stream().filter(s -> s.startsWith(last)).toList();
        }
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : LoftSpec.INSTANCE.params()) {
            String option = p.name() + "=";
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
