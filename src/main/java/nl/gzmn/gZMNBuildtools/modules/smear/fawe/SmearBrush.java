package nl.gzmn.gZMNBuildtools.modules.smear.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.smear.engine.SmearEngine;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.function.Supplier;

/** Smears the leading surface of the brushed area along the stroke direction. */
public class SmearBrush implements PreviewableBrush {

    private final Supplier<Player> player;
    private final int amount;
    private final String direction;

    public SmearBrush(Supplier<Player> player, int amount, String direction) {
        this.player = player;
        this.amount = amount;
        this.direction = direction;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        for (Map.Entry<SmearEngine.Cell, SmearEngine.Cell> e : plan(editSession, position, size).changes().entrySet()) {
            SmearEngine.Cell src = e.getValue();
            editSession.setBlock(e.getKey().x(), e.getKey().y(), e.getKey().z(),
                    editSession.getBlock(src.x(), src.y(), src.z()));
        }
    }

    @Override
    public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        for (Map.Entry<SmearEngine.Cell, SmearEngine.Cell> e : plan(editSession, position, size).changes().entrySet()) {
            SmearEngine.Cell src = e.getValue();
            sink.accept(e.getKey().x(), e.getKey().y(), e.getKey().z(),
                    editSession.getBlock(src.x(), src.y(), src.z()));
        }
    }

    private SmearEngine.Plan plan(EditSession editSession, BlockVector3 position, double size) {
        int[] dir = direction();
        return SmearEngine.smear((x, y, z) -> {
            var state = editSession.getBlock(x, y, z);
            return state.getBlockType().getMaterial().isAir() ? null : state.getBlockType().id();
        }, new int[]{position.x(), position.y(), position.z()}, size, dir, amount);
    }

    private int[] direction() {
        if (!direction.equals("stroke")) {
            return switch (direction) {
                case "north" -> new int[]{0, 0, -1};
                case "south" -> new int[]{0, 0, 1};
                case "west" -> new int[]{-1, 0, 0};
                case "east" -> new int[]{1, 0, 0};
                case "up" -> new int[]{0, 1, 0};
                case "down" -> new int[]{0, -1, 0};
                default -> new int[]{1, 0, 0};
            };
        }
        Player p = player.get();
        Vector v = p == null ? new Vector(1, 0, 0) : p.getLocation().getDirection();
        v.setY(0);
        if (v.lengthSquared() < 1e-6) {
            return new int[]{1, 0, 0};
        }
        v.normalize();
        if (Math.abs(v.getX()) >= Math.abs(v.getZ())) {
            return new int[]{(int) Math.signum(v.getX()), 0, 0};
        }
        return new int[]{0, 0, (int) Math.signum(v.getZ())};
    }
}
