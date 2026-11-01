package nl.gzmn.gZMNBuildtools.modules.splines.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.modules.splines.engine.SplineType;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.shapes.Splines;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PartPlacer;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * {@code /gzspline}: collect points by looking at blocks, then build a tube,
 * wall or road along a smooth curve through them.
 */
public class SplineCommand {

    /** Splines share the generator permission. */
    public static final String PERMISSION = "gzmnbuildtools.generate";

    private record Points(String world, List<int[]> list) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final BlockKnowledge blocks;
    private final Map<UUID, Points> points = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> showing = new ConcurrentHashMap<>();

    public SplineCommand(JavaPlugin plugin, Messages messages, PreviewService previews, BlockKnowledge blocks) {
        this.plugin = plugin;
        this.messages = messages;
        this.previews = previews;
        this.blocks = blocks;
    }

    private List<int[]> pointsOf(Player player) {
        Points p = points.get(player.getUniqueId());
        if (p == null || !p.world.equals(player.getWorld().getName())) {
            p = new Points(player.getWorld().getName(), new ArrayList<>());
            points.put(player.getUniqueId(), p);
        }
        return p.list;
    }

    public void help(Player player) {
        List<int[]> list = pointsOf(player);
        messages.info(player, "Spline: %d point(s). Look at a block and /gzspline add (or /gzspline here).",
                list.size());
        for (SplineType type : SplineType.values()) {
            player.sendMessage(Component.text("  " + type.id(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + type.description(), NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/gzspline build " + type.id() + " ")));
        }
        messages.info(player, "Also: show, undo, clear, list. Add -p to a build to preview it.");
    }

    public void add(Player player, boolean atFeet) {
        Block block = atFeet ? player.getLocation().getBlock().getRelative(0, -1, 0)
                : player.getTargetBlockExact(200);
        if (block == null || block.getType().isAir()) {
            messages.error(player, "Look at a block to add it as a point (or use /gzspline here).");
            return;
        }
        List<int[]> list = pointsOf(player);
        list.add(new int[]{block.getX(), block.getY(), block.getZ()});
        player.spawnParticle(Particle.FLAME, block.getX() + 0.5, block.getY() + 1.1, block.getZ() + 0.5, 12, 0.2, 0.2,
                0.2, 0.01);
        messages.success(player, "Point %d at %d, %d, %d.", list.size(), block.getX(), block.getY(), block.getZ());
        if (list.size() >= 2) {
            show(player, 3);
        }
    }

    public void undo(Player player) {
        List<int[]> list = pointsOf(player);
        if (list.isEmpty()) {
            messages.error(player, "No points to remove.");
            return;
        }
        int[] removed = list.remove(list.size() - 1);
        messages.success(player, "Removed point %d, %d, %d (%d left).", removed[0], removed[1], removed[2], list.size());
    }

    public void clear(Player player) {
        pointsOf(player).clear();
        messages.success(player, "Spline points cleared.");
    }

    public void list(Player player) {
        List<int[]> list = pointsOf(player);
        if (list.isEmpty()) {
            messages.info(player, "No points yet.");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            int[] p = list.get(i);
            messages.info(player, "  %d: %d, %d, %d", i + 1, p[0], p[1], p[2]);
        }
    }

    /** Draws the curve with particles (visible only to the player) for a few seconds. */
    public void show(Player player, int seconds) {
        List<int[]> list = new ArrayList<>(pointsOf(player));
        if (list.size() < 2) {
            messages.error(player, "Add at least two points first.");
            return;
        }
        List<double[]> samples = Splines.sample(list, 1.5);
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
            for (int[] p : list) {
                player.spawnParticle(Particle.FLAME, p[0] + 0.5, p[1] + 1.2, p[2] + 0.5, 2, 0.1, 0.1, 0.1, 0);
            }
        }, 0L, 10L);
        showing.put(player.getUniqueId(), task);
    }

    public void build(Player player, String typeId, String rawRest) {
        List<String> tokens = new ArrayList<>(Arrays.asList(rawRest.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        String rest = String.join(" ", tokens);
        SplineType type = SplineType.byId(typeId).orElse(null);
        if (type == null) {
            messages.error(player, "Unknown spline shape '%s'. Use tube, wall, road or river.", typeId);
            return;
        }
        List<int[]> list = new ArrayList<>(pointsOf(player));
        if (list.size() < 2) {
            messages.error(player, "Add at least two points first (/gzspline add while looking at a block).");
            return;
        }
        BrushArguments<SplineType> args;
        try {
            args = BrushArguments.parse(type, rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        PartPlacer placer;
        try {
            placer = new PartPlacer(List.of(parsePattern(actor, session, args.pattern())));
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        }
        List<double[]> samples = Splines.sample(list, 3);
        int[] count = {0};
        type.build(args, samples, (x, y, z, part, axis) -> count[0]++);
        int max = Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000));
        if (count[0] > max) {
            messages.error(player, "That %s has %d blocks, above the limit of %d (generation.max-blocks).",
                    type.id(), count[0], max);
            return;
        }
        List<int[]> parts = new ArrayList<>(count[0]);
        Splines.RiverFlow flow = type.build(args, samples,
                (x, y, z, part, axis) -> parts.add(new int[]{x, y, z, part, axis}));
        int minY = player.getWorld().getMinHeight();
        int maxY = player.getWorld().getMaxHeight() - 1;
        String label = "a " + type.id() + " through " + list.size() + " points";
        PlayerEdits.async(player, editSession -> {
            Placement placement = type == SplineType.RIVER ? riverPlacement(editSession, parts, placer)
                    : (x, y, z, part, axis) -> type == SplineType.ROAD && part == Splines.ROAD_CLEAR ? AIR
                    : placer.state(x, y, z, part, axis);
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (int[] p : parts) {
                if (p[1] >= minY && p[1] <= maxY) {
                    BlockState state = placement.state(p[0], p[1], p[2], p[3], (char) p[4]);
                    if (state != null) {
                        changes.put(BlockVector3.at(p[0], p[1], p[2]), state);
                    }
                }
            }
            if (preview) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes), label,
                        () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Built %s (%d blocks). //undo reverts it.", label, changes.size());
            if (flow != null) {
                messages.info(player, "The river flows from y=%d down to y=%d%s.", flow.sourceLevel(),
                        flow.mouthLevel(), flow.reversed() ? " (from your last point to your first)" : "");
            }
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Spline build failed for " + player.getName(), e);
            messages.error(player, "Building failed: %s", e.getMessage());
        });
    }

    /** Picks the block for a generated part, or null to leave the world as it is. */
    @FunctionalInterface
    private interface Placement {
        BlockState state(int x, int y, int z, int part, char axis);
    }

    private static final BlockState AIR = BlockTypes.AIR.getDefaultState();

    /**
     * River parts depend on what is there: the valley cut leaves air and
     * existing water alone, and banks only fill gaps. Banks are made of the
     * ground the river dug through most.
     */
    private Placement riverPlacement(EditSession world, List<int[]> parts, PartPlacer placer) {
        Map<BlockType, Integer> dug = new HashMap<>();
        for (int[] p : parts) {
            if (p[3] != Splines.RIVER_BANK) {
                BlockType existing = world.getBlock(p[0], p[1], p[2]).getBlockType();
                String id = existing.id().replace("minecraft:", "");
                if (existing.getMaterial().isSolid() && blocks.rules().isTerrain(id)) {
                    dug.merge(existing == BlockTypes.GRASS_BLOCK ? BlockTypes.DIRT : existing, 1, Integer::sum);
                }
            }
        }
        BlockState bank = dug.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getDefaultState()).orElse(null);
        BlockState water = BlockTypes.WATER.getDefaultState();
        return (x, y, z, part, axis) -> {
            BlockMaterial existing = world.getBlock(x, y, z).getBlockType().getMaterial();
            return switch (part) {
                case Splines.RIVER_BED -> placer.state(x, y, z, 0, axis);
                case Splines.RIVER_WATER -> water;
                case Splines.RIVER_CLEAR -> existing.isAir() || existing.isLiquid() ? null : AIR;
                case Splines.RIVER_BANK -> existing.isAir() || (!existing.isSolid() && !existing.isLiquid())
                        ? (bank != null ? bank : placer.state(x, y, z, 0, axis)) : null;
                default -> null;
            };
        };
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied %d blocks. //undo reverts it.", changes.size());
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

    public List<String> argumentSuggestions(String typeId, String typed) {
        SplineType type = SplineType.byId(typeId).orElse(null);
        if (type == null) {
            return List.of();
        }
        if (!typed.contains(" ")) {
            List<String> patterns = type == SplineType.RIVER ? List.of("gravel", "sand", "clay", "mud", "70%gravel,30%sand")
                    : List.of("stone_bricks", "dirt_path", "cobblestone", "glass", "#stripes[y][1][stone][andesite]");
            return patterns.stream().filter(s -> s.startsWith(typed.toLowerCase(Locale.ROOT))).toList();
        }
        String prefix = typed.substring(0, typed.lastIndexOf(' ') + 1);
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : type.params()) {
            String option = p.name() + "=";
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        if ("-p".startsWith(current) && !typed.contains("-p")) {
            out.add(prefix + "-p");
        }
        return out;
    }
}
