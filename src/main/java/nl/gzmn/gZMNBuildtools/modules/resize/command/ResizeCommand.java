package nl.gzmn.gZMNBuildtools.modules.resize.command;

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
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.resize.engine.ResizePlanner;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
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
import java.util.Map;
import java.util.logging.Level;

/** {@code /gzresize}: scales the cuboid selection by nearest-neighbour sampling. */
public class ResizeCommand {

    public static final String PERMISSION = "gzmnbuildtools.resize";

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final Limits limits;

    public ResizeCommand(JavaPlugin plugin, Messages messages, PreviewService previews, Limits limits) {
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
        messages.info(player, "Usage: /gzresize <scale|sx,sy,sz> [pattern] [-p] (scale 0.25-4 per axis)");
    }

    public void resize(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        if (tokens.isEmpty()) {
            usage(player);
            return;
        }
        String scaleText = tokens.remove(0);
        double[] scale = parseScale(scaleText);
        if (scale == null) {
            messages.error(player, "Scale must be a number or sx,sy,sz between 0.25 and 4 (e.g. /gzresize 2 or 2,1,0.5).");
            return;
        }
        String patternText = tokens.isEmpty() ? null : String.join(" ", tokens);

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
                messages.error(player, "Resize needs a cuboid selection; set the two corners with //pos1 and //pos2.");
                return;
            }
            cuboid = c;
        } catch (IncompleteRegionException e) {
            messages.error(player, "Resize needs a cuboid selection; set the two corners with //pos1 and //pos2.");
            return;
        }
        final CuboidRegion sel = cuboid;
        final Pattern pat = pattern;
        final boolean prev = preview;
        Bounds bounds = new Bounds(Math.min(sel.getPos1().x(), sel.getPos2().x()),
                Math.min(sel.getPos1().y(), sel.getPos2().y()), Math.min(sel.getPos1().z(), sel.getPos2().z()),
                Math.max(sel.getPos1().x(), sel.getPos2().x()), Math.max(sel.getPos1().y(), sel.getPos2().y()),
                Math.max(sel.getPos1().z(), sel.getPos2().z()));
        ResizePlanner.Plan plan = ResizePlanner.plan(bounds, scale[0], scale[1], scale[2]);
        if (plan.problem() != null) {
            messages.error(player, "%s", plan.problem());
            return;
        }
        if (plan.blockCount() > maxBlocks(player)) {
            messages.error(player, "That resize makes %d blocks, above your limit of %d.", plan.blockCount(),
                    maxBlocks(player));
            return;
        }
        int minY = world.getMinY();
        int maxY = world.getMaxY();
        PlayerEdits.async(player, editSession -> {
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (Map.Entry<ResizePlanner.Cell, ResizePlanner.Cell> e : plan.placements().entrySet()) {
                ResizePlanner.Cell out = e.getKey();
                if (out.y() < minY || out.y() > maxY) {
                    continue;
                }
                ResizePlanner.Cell src = e.getValue();
                BlockState state = editSession.getBlock(src.x(), src.y(), src.z());
                if (pat != null) {
                    var applied = pat.applyBlock(BlockVector3.at(src.x(), src.y(), src.z()));
                    if (applied != null) {
                        state = StateMapper.map(state, applied.getBlockType());
                    }
                }
                changes.put(BlockVector3.at(out.x(), out.y(), out.z()), state);
            }
            if (prev) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(changes),
                        "the resized selection", () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Resized the selection to %s: %d blocks. //undo reverts it.",
                    plan.out(), changes.size());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Resize failed for " + player.getName(), e);
            messages.error(player, "Resize failed: %s", e.getMessage());
        });
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the resize: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }

    private static double[] parseScale(String text) {
        String[] parts = text.split(",");
        if (parts.length > 3) {
            return null;
        }
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (out[i] < 0.25 || out[i] > 4) {
                return null;
            }
        }
        while (out.length < 3) {
            out = Arrays.copyOf(out, out.length + 1);
            out[out.length - 1] = out[0];
        }
        return out;
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
}
