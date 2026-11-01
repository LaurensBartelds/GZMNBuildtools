package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Small helpers for tools that edit the world on behalf of a player through FAWE. */
public final class PlayerEdits {

    private PlayerEdits() {
    }

    /** The block the player is looking at, within {@code range} blocks. */
    public static Optional<BlockVector3> lookedAt(Player player, int range) {
        Block block = player.getTargetBlockExact(range);
        if (block == null || block.getType().isAir()) {
            return Optional.empty();
        }
        return Optional.of(BlockVector3.at(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * Runs {@code edit} in an edit session for the player on FAWE's queue. When
     * it returns true the edit is remembered for //undo.
     */
    public static void async(Player player, Function<EditSession, Boolean> edit, Consumer<RuntimeException> onError) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                if (edit.apply(editSession)) {
                    session.remember(editSession);
                }
            } catch (RuntimeException e) {
                onError.accept(e);
            }
        });
    }

    /** Client-side ghost blocks for previewing collected changes. */
    public static Map<Position, BlockData> ghost(Map<BlockVector3, BlockState> changes) {
        Map<Position, BlockData> ghost = new HashMap<>();
        changes.forEach((pos, state) -> ghost.put(Position.block(pos.x(), pos.y(), pos.z()), BukkitAdapter.adapt(state)));
        return ghost;
    }
}
