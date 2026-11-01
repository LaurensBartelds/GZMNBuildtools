package nl.gzmn.gZMNBuildtools.modules.selection.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /gzselect}: makes the WorldEdit selection the bounding box of the
 * build you are looking at, so every normal command works on "this build".
 */
public class SelectCommand {

    public static final String PERMISSION = "gzmnbuildtools.select";
    private static final int RANGE = 200;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BuildEditor editor;

    public SelectCommand(JavaPlugin plugin, Messages messages, BuildEditor editor) {
        this.plugin = plugin;
        this.messages = messages;
        this.editor = editor;
    }

    /** What counts as the build for a selection mode name. */
    public static Optional<TargetMode> mode(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "build", "structure" -> Optional.of(TargetMode.STRUCTURAL);
            case "material", "clicked", "same" -> Optional.of(TargetMode.CLICKED);
            case "all", "everything" -> Optional.of(TargetMode.ALL);
            default -> Optional.empty();
        };
    }

    public static List<String> modes() {
        return List.of("build", "material", "all");
    }

    /** Expands bounds by {@code pad} blocks on every side. */
    public static Bounds pad(Bounds b, int pad) {
        return new Bounds(b.minX() - pad, b.minY() - pad, b.minZ() - pad, b.maxX() + pad, b.maxY() + pad,
                b.maxZ() + pad);
    }

    public void select(Player player, String modeName, int pad) {
        Optional<TargetMode> mode = mode(modeName);
        if (mode.isEmpty()) {
            messages.error(player, "Unknown mode '%s'. Use build, material or all.", modeName);
            return;
        }
        Optional<BlockVector3> target = PlayerEdits.lookedAt(player, RANGE);
        if (target.isEmpty()) {
            messages.error(player, "Look at a block of the build you want to select.");
            return;
        }
        PlayerEdits.async(player, editSession -> {
            BuildEditor.Scan found = editor.scan(editSession, target.get(), mode.get(), List.of());
            if (!found.ok()) {
                messages.error(player, "%s", found.problem());
                return false;
            }
            BuildScan scan = found.scan();
            Bounds box = pad(scan.bounds(), pad);
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
            session.setRegionSelector(actor.getWorld(), new CuboidRegionSelector(actor.getWorld(),
                    BlockVector3.at(box.minX(), box.minY(), box.minZ()),
                    BlockVector3.at(box.maxX(), box.maxY(), box.maxZ())));
            session.dispatchCUISelection(actor);
            messages.success(player, "Selected the %s build at %s%s: %d blocks (%s).", box.toString(),
                    box.minX() + "," + box.minY() + "," + box.minZ(), pad > 0 ? " (+" + pad + " around it)" : "",
                    scan.size(), materials(scan));
            if (scan.truncated()) {
                messages.info(player, "It is larger than the scan limit; the selection covers the scanned part.");
            }
            player.sendMessage(Component.text("  [//copy]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("//copy"))
                    .append(Component.text("  [//cut]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("//cut")))
                    .append(Component.text("  [//count]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand("//count ")))
                    .append(Component.text("  [//replace]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand("//replace "))));
            Bukkit.getScheduler().runTask(plugin, () -> outline(player, box));
            return false;
        }, e -> messages.error(player, "Could not select the build: %s", e.getMessage()));
    }

    private static String materials(BuildScan scan) {
        int total = scan.groups().values().stream().mapToInt(Integer::intValue).sum();
        return scan.rankedGroups().stream().limit(3)
                .map(e -> e.getKey() + " " + Math.round(100.0 * e.getValue() / Math.max(1, total)) + "%")
                .collect(Collectors.joining(", "));
    }

    /** Draws the box's edges with particles only the player sees, for a few seconds. */
    private void outline(Player player, Bounds box) {
        int[] runs = {10};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || runs[0]-- <= 0) {
                task[0].cancel();
                return;
            }
            double x0 = box.minX();
            double y0 = box.minY();
            double z0 = box.minZ();
            double x1 = box.maxX() + 1;
            double y1 = box.maxY() + 1;
            double z1 = box.maxZ() + 1;
            double step = Math.max(1, Math.max(x1 - x0, Math.max(y1 - y0, z1 - z0)) / 40);
            for (double[] edge : new double[][]{
                    {x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}}) {
                double length = Math.max(Math.abs(edge[3] - edge[0]), Math.max(Math.abs(edge[4] - edge[1]),
                        Math.abs(edge[5] - edge[2])));
                for (double d = 0; d <= length; d += step) {
                    double t = length == 0 ? 0 : d / length;
                    player.spawnParticle(Particle.END_ROD, edge[0] + (edge[3] - edge[0]) * t,
                            edge[1] + (edge[4] - edge[1]) * t, edge[2] + (edge[5] - edge[2]) * t, 1, 0, 0, 0, 0);
                }
            }
        }, 0L, 10L);
    }
}
