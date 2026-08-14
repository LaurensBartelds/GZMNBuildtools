package nl.gzmn.gZMNBuildtools.command.brush;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BrushCommand {

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BrushUIManager uiManager;

    public BrushCommand(JavaPlugin plugin, Messages messages, BrushUIManager uiManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.uiManager = uiManager;
    }

    public void executeGui(Player player) {
        uiManager.openMainGui(player);
    }

    public void executeCommand(Player player, String shape, int radius, String code) {
        String pattern = CodeDecoder.decodeToPattern(code);

        if (pattern == null) {
            messages.error(player, "Invalid or corrupted brush code.");
            return;
        }

        applyBrush(player, shape, radius, pattern);
    }

    public void applyBrush(Player player, String shape, int radius, String pattern) {
        // We dispatch the FAWE/WE command directly to the player
        String command = String.format("brush %s %s %d", shape, pattern, radius);

        // Use Bukkit to dispatch command as the player, which hooks nicely into FAWE
        Bukkit.dispatchCommand(player, command);

        messages.success(player, "Custom gradient brush bound to your current item!");
    }
}
