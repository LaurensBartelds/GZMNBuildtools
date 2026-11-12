package nl.gzmn.gZMNBuildtools.modules.revolve.command;

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
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.revolve.engine.RevolvePlanner;
import nl.gzmn.gZMNBuildtools.modules.revolve.engine.RevolveSpec;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import nl.gzmn.gZMNBuildtools.shared.worldedit.StateMapper;
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
import java.util.logging.Level;

/** {@code /gzrevolve}: copies the cuboid selection around an axis through point 1. */
public class RevolveCommand {

    public static final String PERMISSION = "gzmnbuildtools.revolve";

    private static final Map<String, String> FACING_Y = Map.of("north", "east", "east", "south",
            "south", "west", "west", "north");
    private static final Map<String, String> FACING_X = Map.of("north", "up", "up", "south",
            "south", "down", "down", "north");
    private static final Map<String, String> FACING_Z = Map.of("east", "up", "up", "west",
            "west", "down", "down", "east");
    private static final Map<String, String> AXIS_Y = Map.of("x", "z", "z", "x");
    private static final Map<String, String> AXIS_X = Map.of("y", "z", "z", "y");
    private static final Map<String, String> AXIS_Z = Map.of("x", "y", "y", "x");

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final Limits limits;

    public RevolveCommand(JavaPlugin plugin, Messages messages, PreviewService previews, Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.previews = previews;
        this.limits = limits;
    }

    private int maxBlocks(Player player) {
        return limits.get(player, Limits.Kind.BLOCKS,
                Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000)));
    }

    public void usage(Player player) {
        messages.info(player, "Usage: %s", RevolveSpec.INSTANCE.usage());
        for (BrushParam p : RevolveSpec.INSTANCE.params()) {
            messages.info(player, "  %s (default %s): %s%s", p.name(), p.display(p.defaultValue()),
                    p.description(), p.isChoice() ? " [" + String.join(", ", p.choices()) + "]" : "");
        }
    }

    public void revolve(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        if (tokens.isEmpty()) {
            usage(player);
            return;
        }
        String patternText = null;
        if (!tokens.get(0).contains("=") && !isNumber(tokens.get(0))) {
            patternText = tokens.remove(0);
        }
        String rest = String.join(" ", tokens);
        BrushArguments<RevolveSpec> args;
        try {
            args = BrushArguments.parse(RevolveSpec.INSTANCE, rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        char axis = args.choice("axis").charAt(0);
        double degrees = args.get("degrees");
        int steps = args.getInt("steps");

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World world = actor.getWorld();

        Pattern pattern = null;
        if (patternText != null) {
            try {
                pattern = parsePattern(actor, session, patternText);
            } catch (InputParseException e) {
                messages.error(player, "Invalid pattern: %s", e.getMessage());
                return;
            }
        }

        CuboidRegion cuboid;
        try {
            Region selection = session.getSelection(world);
            if (!(selection instanceof CuboidRegion c)) {
                messages.error(player, "Revolve needs a cuboid selection; set the two corners with //pos1 and //pos2.");
                return;
            }
            cuboid = c;
        } catch (IncompleteRegionException e) {
            messages.error(player, "Revolve needs a cuboid selection; set the two corners with //pos1 and //pos2.");
            return;
        }
        final CuboidRegion sel = cuboid;
        final Pattern pat = pattern;
        final char ax = axis;
        final double deg = degrees;
        final int st = steps;
        final boolean prev = preview;

        Bounds bounds = new Bounds(Math.min(sel.getPos1().x(), sel.getPos2().x()),
                Math.min(sel.getPos1().y(), sel.getPos2().y()), Math.min(sel.getPos1().z(), sel.getPos2().z()),
                Math.max(sel.getPos1().x(), sel.getPos2().x()), Math.max(sel.getPos1().y(), sel.getPos2().y()),
                Math.max(sel.getPos1().z(), sel.getPos2().z()));
        int[] center = {sel.getPos1().x(), sel.getPos1().y(), sel.getPos1().z()};
        RevolvePlanner.Plan plan = RevolvePlanner.plan(bounds, center, ax, deg, st);
        if (plan.problem() != null) {
            messages.error(player, "%s", plan.problem());
            return;
        }
        if (plan.blockCount() > maxBlocks(player)) {
            messages.error(player, "That revolve places %d blocks, above your limit of %d.", plan.blockCount(),
                    maxBlocks(player));
            return;
        }

        int minY = world.getMinY();
        int maxY = world.getMaxY();
        PlayerEdits.async(player, editSession -> {
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (Map.Entry<RevolvePlanner.Cell, RevolvePlanner.Placement> e : plan.placements().entrySet()) {
                RevolvePlanner.Cell out = e.getKey();
                RevolvePlanner.Placement p = e.getValue();
                if (out.y() < minY || out.y() > maxY) {
                    continue;
                }
                BlockState state = editSession.getBlock(p.source().x(), p.source().y(), p.source().z());
                if (pat != null) {
                    var applied = pat.applyBlock(BlockVector3.at(p.source().x(), p.source().y(), p.source().z()));
                    if (applied != null) {
                        state = StateMapper.map(state, applied.getBlockType());
                    }
                }
                state = rotate(state, ax, p.quarterTurns());
                changes.put(BlockVector3.at(out.x(), out.y(), out.z()), state);
            }
            if (prev) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes),
                        "the revolved selection", () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Revolved the selection: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Revolve failed for " + player.getName(), e);
            messages.error(player, "Revolve failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the revolve: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }

    private static BlockState rotate(BlockState state, char axis, int quarterTurns) {
        BlockState out = state;
        for (int i = 0; i < quarterTurns; i++) {
            out = withString(out, "facing", cycle(facingOf(out), axis));
            out = withString(out, "axis", cycleAxis(axisOf(out), axis));
        }
        return out;
    }

    private static String facingOf(BlockState state) {
        Property<?> p = state.getBlockType().getProperty("facing");
        if (p == null || state.getState(p) == null) {
            return null;
        }
        return state.getState(p).toString();
    }

    private static String axisOf(BlockState state) {
        Property<?> p = state.getBlockType().getProperty("axis");
        if (p == null || state.getState(p) == null) {
            return null;
        }
        return state.getState(p).toString();
    }

    private static String cycle(String value, char axis) {
        if (value == null) {
            return null;
        }
        Map<String, String> map = switch (axis) {
            case 'x' -> value.equals("east") || value.equals("west") ? Map.of(value, value) : FACING_X;
            case 'z' -> value.equals("north") || value.equals("south") ? Map.of(value, value) : FACING_Z;
            default -> FACING_Y;
        };
        return map.getOrDefault(value, value);
    }

    private static String cycleAxis(String value, char axis) {
        if (value == null) {
            return null;
        }
        Map<String, String> map = switch (axis) {
            case 'x' -> AXIS_X;
            case 'z' -> AXIS_Z;
            default -> AXIS_Y;
        };
        return map.getOrDefault(value, value);
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

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
            return List.of("stone_bricks", "oak_planks", "glass", "deepslate_bricks")
                    .stream().filter(s -> s.startsWith(last)).toList();
        }
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : RevolveSpec.INSTANCE.params()) {
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
