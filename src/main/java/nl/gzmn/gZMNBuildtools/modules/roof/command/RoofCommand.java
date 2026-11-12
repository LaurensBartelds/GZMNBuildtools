package nl.gzmn.gZMNBuildtools.modules.roof.command;

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
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofOptions;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofPlanner;
import nl.gzmn.gZMNBuildtools.modules.roof.engine.RoofSpec;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** {@code /gzroof}: puts a gable, hip, pyramid or flat roof on a selection or the build you look at. */
public class RoofCommand {

    public static final String PERMISSION = "gzmnbuildtools.roof";

    private static final int RANGE = 200;

    /** Where the roof area comes from. */
    public enum Mode {
        SELECTION_OR_BUILD,
        BUILD
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BuildEditor editor;
    private final PreviewService previews;
    private final Limits limits;

    public RoofCommand(JavaPlugin plugin, Messages messages, BuildEditor editor, PreviewService previews,
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
        messages.info(player, "Usage: %s", RoofSpec.INSTANCE.usage());
        for (BrushParam p : RoofSpec.INSTANCE.params()) {
            messages.info(player, "  %s (default %s): %s%s", p.name(), p.display(p.defaultValue()),
                    p.description(), p.isChoice() ? " [" + String.join(", ", p.choices()) + "]" : "");
        }
    }

    public void roof(Player player, String rawArgs, Mode mode, boolean forcePreview) {
        roof(player, rawArgs, mode, forcePreview, ThreadLocalRandom.current().nextLong());
    }

    public void roof(Player player, String rawArgs, Mode mode, boolean forcePreview, long seed) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = forcePreview
                | tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        String rest = String.join(" ", tokens);
        if (rest.isBlank()) {
            messages.error(player, "Usage: %s", RoofSpec.INSTANCE.usage());
            return;
        }
        BrushArguments<RoofSpec> args;
        try {
            args = BrushArguments.parse(RoofSpec.INSTANCE, rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        RoofOptions options = new RoofOptions(args.choice("style"), args.choice("axis"), args.getInt("height"),
                args.getInt("overhang"), args.get("noise"), args.get("stack-down") >= 0.5,
                args.get("level-ends") >= 0.5, seed);

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
        if (mode == Mode.SELECTION_OR_BUILD) {
            try {
                Region selection = session.getSelection(world);
                if (selection instanceof CuboidRegion c) {
                    cuboid = c;
                }
            } catch (IncompleteRegionException ignored) {
            }
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
        PlayerEdits.async(player, editSession -> {
            Bounds bounds;
            RoofPlanner.ColumnTops tops;
            String label;
            if (selection != null) {
                bounds = new Bounds(Math.min(selection.getPos1().x(), selection.getPos2().x()), world.getMinY(),
                        Math.min(selection.getPos1().z(), selection.getPos2().z()),
                        Math.max(selection.getPos1().x(), selection.getPos2().x()), world.getMaxY(),
                        Math.max(selection.getPos1().z(), selection.getPos2().z()));
                ExtentBlockSource source = new ExtentBlockSource(editSession);
                tops = (x, z) -> {
                    for (int y = source.maxY(); y >= source.minY(); y--) {
                        if (source.blockAt(x, y, z) != null) {
                            return y;
                        }
                    }
                    return -1;
                };
                label = "the selection";
            } else {
                BuildEditor.Scan found = editor.scan(editSession, target, TargetMode.BUILD, List.of());
                if (!found.ok()) {
                    messages.error(player, "%s", found.problem());
                    return false;
                }
                BuildScan scan = found.scan();
                bounds = scan.bounds();
                tops = (x, z) -> {
                    for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
                        if (scan.contains(x, y, z)) {
                            return y;
                        }
                    }
                    return -1;
                };
                label = "this " + bounds + " build";
            }
            RoofPlanner.Plan plan = RoofPlanner.plan(bounds, tops, options);
            if (plan.problem() != null) {
                messages.error(player, "%s", plan.problem());
                return false;
            }
            if (plan.blockCount() > maxBlocks(player)) {
                messages.error(player, "That roof has %d blocks, above your limit of %d.",
                        plan.blockCount(), maxBlocks(player));
                return false;
            }
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (Map.Entry<RoofPlanner.Cell, RoofPlanner.Part> entry : plan.placements().entrySet()) {
                RoofPlanner.Cell cell = entry.getKey();
                BlockState state = stateFor(pattern, entry.getValue(), cell, world);
                if (state != null) {
                    changes.put(BlockVector3.at(cell.x(), cell.y(), cell.z()), state);
                }
            }
            if (preview) {
                Map<Position, BlockData> ghost = PlayerEdits.ghost(changes);
                Bukkit.getScheduler().runTask(plugin, () ->
                        previews.show(player, ghost, "a " + options.style() + " roof on " + label,
                                () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Placed a %s roof (%d blocks) on %s. //undo reverts it.",
                    options.style(), changes.size(), label);
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Roof failed for " + player.getName(), e);
            messages.error(player, "Roof failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the roof: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }

    private BlockState stateFor(Pattern pattern, RoofPlanner.Part part, RoofPlanner.Cell cell, World world) {
        if (cell.y() < world.getMinY() || cell.y() > world.getMaxY()) {
            return null;
        }
        BaseBlock applied = pattern.applyBlock(BlockVector3.at(cell.x(), cell.y(), cell.z()));
        if (applied == null) {
            return null;
        }
        BlockState base = applied.toImmutableState();
        if (part.kind() == ShapeKind.FULL) {
            return base;
        }
        String id = ExtentBlockSource.idOf(base);
        String plain = id == null ? null : ShapeKind.fromSuffix(id)
                .map(kind -> id.substring(0, id.length() - kind.suffix().length()))
                .orElse(id);
        String variant = plain == null ? null : editor.blocks().variants().resolve(plain, part.kind()).orElse(null);
        BlockType type = variant == null ? null : editor.blocks().type(variant);
        if (type == null) {
            return base;
        }
        BlockState state = type.getDefaultState();
        if (part.kind() == ShapeKind.STAIRS) {
            state = withString(state, "facing", part.facing());
            state = withString(state, "half", "bottom");
            state = withString(state, "shape", "straight");
            state = withString(state, "waterlogged", "false");
        } else if (part.kind() == ShapeKind.SLAB) {
            state = withString(state, "type", part.topHalf() ? "top" : "bottom");
            state = withString(state, "waterlogged", "false");
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static BlockState withString(BlockState state, String name, String value) {
        if (value == null) {
            return state;
        }
        Property<Object> property = state.getBlockType().getProperty(name);
        if (property == null) {
            return state;
        }
        for (Object v : property.getValues()) {
            if (v.toString().equals(value)) {
                return state.with(property, v);
            }
        }
        return state;
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
            return List.of("stone_bricks", "stone_brick_stairs", "oak_planks", "deepslate_brick_stairs",
                            "#stripes[y][1][stone][andesite]")
                    .stream().filter(s -> s.startsWith(last)).toList();
        }
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : RoofSpec.INSTANCE.params()) {
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
