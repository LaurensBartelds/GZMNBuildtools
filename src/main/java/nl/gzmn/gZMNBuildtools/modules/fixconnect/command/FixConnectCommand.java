package nl.gzmn.gZMNBuildtools.modules.fixconnect.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.fixconnect.engine.FixConnectPlanner;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
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
import java.util.Map;
import java.util.logging.Level;

/** {@code /gzfix}: reconnect fences, walls and panes in the selection to their neighbours. */
public class FixConnectCommand {

    public static final String PERMISSION = "gzmnbuildtools.fixconnect";

    private static final String[] DIRS = FixConnectPlanner.directionNames();

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final Limits limits;
    private final BlockKnowledge blocks;

    public FixConnectCommand(JavaPlugin plugin, Messages messages, PreviewService previews, Limits limits,
                             BlockKnowledge blocks) {
        this.plugin = plugin;
        this.messages = messages;
        this.previews = previews;
        this.limits = limits;
        this.blocks = blocks;
    }

    private int maxBlocks(Player player) {
        return limits.get(player, Limits.Kind.BLOCKS,
                Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000)));
    }

    public void usage(Player player) {
        messages.info(player, "Usage: %s", "/gzfix [-p]");
    }

    public void fix(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        if (!tokens.isEmpty()) {
            usage(player);
            return;
        }

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World world = actor.getWorld();
        CuboidRegion cuboid;
        try {
            Region selection = session.getSelection(world);
            if (!(selection instanceof CuboidRegion c)) {
                messages.error(player, "Fix connections needs a cuboid selection; set the two corners with //pos1 and //pos2.");
                return;
            }
            cuboid = c;
        } catch (IncompleteRegionException e) {
            messages.error(player, "Fix connections needs a cuboid selection; set the two corners with //pos1 and //pos2.");
            return;
        }
        final CuboidRegion sel = cuboid;
        final boolean prev = preview;
        PlayerEdits.async(player, editSession -> {
            Bounds bounds = new Bounds(Math.min(sel.getPos1().x(), sel.getPos2().x()),
                    Math.min(sel.getPos1().y(), sel.getPos2().y()), Math.min(sel.getPos1().z(), sel.getPos2().z()),
                    Math.max(sel.getPos1().x(), sel.getPos2().x()), Math.max(sel.getPos1().y(), sel.getPos2().y()),
                    Math.max(sel.getPos1().z(), sel.getPos2().z()));
            ExtentBlockSource source = new ExtentBlockSource(editSession);
            FixConnectPlanner.Plan plan = FixConnectPlanner.plan(bounds, source::blockAt,
                    id -> {
                        var type = blocks.type(id);
                        return type != null && type.getMaterial().isSolid();
                    },
                    (x, y, z) -> connectionsOf(editSession.getBlock(x, y, z)));
            if (plan.problem() != null) {
                messages.error(player, "%s", plan.problem());
                return false;
            }
            if (plan.blockCount() > maxBlocks(player)) {
                messages.error(player, "That fix touches %d blocks, above your limit of %d.",
                        plan.blockCount(), maxBlocks(player));
                return false;
            }
            Map<BlockVector3, BlockState> changes = new LinkedHashMap<>();
            for (Map.Entry<FixConnectPlanner.Cell, boolean[]> e : plan.changes().entrySet()) {
                FixConnectPlanner.Cell cell = e.getKey();
                BlockState state = editSession.getBlock(cell.x(), cell.y(), cell.z());
                for (int d = 0; d < 4; d++) {
                    state = withBoolean(state, DIRS[d], e.getValue()[d]);
                }
                changes.put(BlockVector3.at(cell.x(), cell.y(), cell.z()), state);
            }
            if (prev) {
                Map<Position, BlockData> ghost = PlayerEdits.ghost(changes);
                Bukkit.getScheduler().runTask(plugin, () ->
                        previews.show(player, ghost, "fixed connections in the selection", () -> apply(player, changes)));
                return false;
            }
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Fixed connections on %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> {
            plugin.getLogger().log(Level.WARNING, "Fix-connect failed for " + player.getName(), e);
            messages.error(player, "Fix-connect failed: %s", e.getMessage());
        });
    }

    private static boolean[] connectionsOf(BlockState state) {
        boolean[] out = new boolean[4];
        for (int d = 0; d < 4; d++) {
            out[d] = booleanValue(state, DIRS[d]);
        }
        return out;
    }

    private static boolean booleanValue(BlockState state, String name) {
        Property<Object> property = property(state, name);
        Object value = property == null ? null : state.getState(property);
        return value instanceof Boolean b && b;
    }

    private static BlockState withBoolean(BlockState state, String name, boolean value) {
        Property<Object> property = property(state, name);
        if (property == null) {
            return state;
        }
        for (Object v : property.getValues()) {
            if (v instanceof Boolean b && b == value) {
                return state.with(property, v);
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static Property<Object> property(BlockState state, String name) {
        return (Property<Object>) state.getBlockType().getProperty(name);
    }

    private void apply(Player player, Map<BlockVector3, BlockState> changes) {
        PlayerEdits.async(player, editSession -> {
            changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Applied the fix: %d blocks. //undo reverts it.", changes.size());
            return true;
        }, e -> messages.error(player, "Applying failed: %s", e.getMessage()));
    }
}
