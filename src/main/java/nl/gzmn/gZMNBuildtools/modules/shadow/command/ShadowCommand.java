package nl.gzmn.gZMNBuildtools.modules.shadow.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.shadow.engine.ShadowPlanner;
import nl.gzmn.gZMNBuildtools.modules.shadow.engine.ShadowSpec;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.ExtentBlockSource;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/** {@code /gzshadow}: casts a shadow of a selection or the build you look at onto the surface around it. */
public class ShadowCommand {

    public static final String PERMISSION = "gzmnbuildtools.shadow";

    private static final int RANGE = 200;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BuildEditor editor;
    private final PreviewService previews;
    private final Limits limits;

    public ShadowCommand(JavaPlugin plugin, Messages messages, BuildEditor editor, PreviewService previews,
                         Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.editor = editor;
        this.previews = previews;
        this.limits = limits;
    }

    private int maxBlocks(Player player) {
        return limits.get(player, Limits.Kind.BLOCKS,
                Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000)));
    }

    public void usage(Player player) {
        messages.info(player, "Usage: %s", ShadowSpec.INSTANCE.usage());
        for (BrushParam p : ShadowSpec.INSTANCE.params()) {
            messages.info(player, "  %s (default %s): %s%s", p.name(), p.display(p.defaultValue()),
                    p.description(), p.isChoice() ? " [" + String.join(", ", p.choices()) + "]" : "");
        }
    }

    public void shadow(Player player, String rawArgs, boolean forcePreview) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = forcePreview
                | tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        String rest = String.join(" ", tokens);
        if (rest.isBlank()) {
            messages.error(player, "Usage: %s", ShadowSpec.INSTANCE.usage());
            return;
        }
        BrushArguments<ShadowSpec> args;
        try {
            args = BrushArguments.parse(ShadowSpec.INSTANCE, rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        String dir = args.choice("dir");
        int dist = args.getInt("dist");
        int[] vec = switch (dir) {
            case "north" -> new int[]{0, 0, -1};
            case "east" -> new int[]{1, 0, 0};
            case "south" -> new int[]{0, 0, 1};
            case "west" -> new int[]{-1, 0, 0};
            default -> new int[]{0, -1, 0};
        };

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World world = actor.getWorld();

        Pattern pattern;
        try {
            pattern = parsePattern(actor, session, args.pattern());
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        }

        CuboidRegion cuboid = null;
        try {
            Region selection = session.getSelection(world);
            if (selection instanceof CuboidRegion c) {
                cuboid = c;
            }
        } catch (IncompleteRegionException ignored) {
        }
        BlockVector3 click = null;
        if (cuboid == null) {
            Optional<BlockVector3> looked = PlayerEdits.lookedAt(player, RANGE);
            if (looked.isEmpty()) {
                messages.error(player, "Look at a block of the build, or select a cuboid area, first.");
                return;
            }
            click = looked.get();
        }

        final CuboidRegion selection = cuboid;
        final BlockVector3 target = click;
        final int dx = vec[0];
        final int dy = vec[1];
        final int dz = vec[2];
        final int distance = dist;
        PlayerEdits.async(player, editSession -> {
            ExtentBlockSource source = new ExtentBlockSource(editSession);
            Bounds bounds;
            Set<ShadowPlanner.Cell> build = new HashSet<>();
            String label;
            if (selection != null) {
                bounds = new Bounds(Math.min(selection.getPos1().x(), selection.getPos2().x()), world.getMinY(),
                        Math.min(selection.getPos1().z(), selection.getPos2().z()),
                        Math.max(selection.getPos1().x(), selection.getPos2().x()), world.getMaxY(),
                        Math.max(selection.getPos1().z(), selection.getPos2().z()));
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                            if (source.blockAt(x, y, z) != null) {
                                build.add(new ShadowPlanner.Cell(x, y, z));
                            }
                        }
                    }
                }
                label = "the selection";
            } else {
                BuildEditor.Scan found = editor.scan(editSession, target, TargetMode.STRUCTURAL, List.of());
                if (!found.ok()) {
                    messages.error(player, "%s", found.problem());
                    return false;
                }
                BuildScan scan = found.scan();
                bounds = scan.bounds();
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                            if (scan.contains(x, y, z)) {
                                build.add(new ShadowPlanner.Cell(x, y, z));
                            }
                        }
                    }
                }
                label = "this " + bounds + " build";
            }
            ShadowPlanner.Plan plan = ShadowPlanner.plan(bounds, build,
                    (x, y, z) -> source.blockAt(x, y, z) != null,
                    dx, dy, dz, distance, world.getMinY(), world.getMaxY());
            if (plan.problem() != null) {
                messages.error(player, "%s", plan.problem());
                return false;
            }
            if (plan.blockCount() > maxBlocks(player)) {
                messages.error(player, "That shadow has %d blocks, above your limit of %d.",
                        plan.blockCount(), maxBlocks(player));
                return false;
            }
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (ShadowPlanner.Cell cell : plan.shadows().keySet()) {
                BaseBlock applied = pattern.applyBlock(BlockVector3.at(cell.x(), cell.y(), cell.z()));
                if (applied != null) {
                    changes.put(BlockVector3.at(cell.x(), cell.y(), cell.z()), applied.toImmutableState());
                }
            }
            if (preview) {
                Map<Position, BlockData> ghost = PlayerEdits.ghost(changes);
                Bukkit.getScheduler().runTask(plugin, () ->
                        previews.show(player, ghost, "a shadow cast " + dir + " from " + label,
                                () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Placed a shadow (%d blocks) on %s. //undo reverts it.",
                    changes.size(), label);
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Shadow failed for " + player.getName(), e);
            messages.error(player, "Shadow failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the shadow: %d blocks. //undo reverts it.", changes.size());
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
            return List.of("black_concrete", "black_stained_glass", "deepslate", "tinted_glass")
                    .stream().filter(s -> s.startsWith(last)).toList();
        }
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : ShadowSpec.INSTANCE.params()) {
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
