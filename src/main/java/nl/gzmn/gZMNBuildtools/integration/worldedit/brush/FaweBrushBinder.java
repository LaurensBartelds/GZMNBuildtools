package nl.gzmn.gZMNBuildtools.integration.worldedit.brush;

import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import org.bukkit.entity.Player;

/** Binds a {@link Brush} to the player's held item as a regular FAWE brush tool. */
public final class FaweBrushBinder {

    private FaweBrushBinder() {
    }

    /**
     * @param label shown by FAWE's brush info (e.g. "gzmn gradient auto")
     * @param fill  fill pattern, or null for brushes that do not use one
     */
    public static void bind(Player player, Brush brush, String permission, double radius, String label,
                            Pattern fill) throws WorldEditException {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        BrushSettings settings = new BrushSettings();
        settings.setBrush(brush);
        settings.addPermission(permission);
        settings.addSetting(BrushSettings.SettingType.BRUSH, label);

        session.setTool(actor, null);
        BrushTool tool = session.getBrushTool(actor);
        tool.setPrimary(settings);
        tool.setSecondary(settings);
        tool.setSize(radius);
        if (fill != null) {
            tool.setFill(fill);
        }
    }
}
