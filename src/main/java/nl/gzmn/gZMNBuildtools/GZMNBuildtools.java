package nl.gzmn.gZMNBuildtools;

import nl.gzmn.gZMNBuildtools.command.CommandRegistry;
import nl.gzmn.gZMNBuildtools.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.ui.typereplace.TypeReplaceUIManager;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class GZMNBuildtools extends JavaPlugin {

    private TypeReplaceUIManager typeReplaceUIManager;
    private GradientStorageManager storageManager;
    private GradientCommand gradientCommand;
    private CommandRegistry commandRegistry;

    public GZMNBuildtools() {
        this.gradientCommand = new GradientCommand();
        TypeReplaceCommand typeReplaceCommand = new TypeReplaceCommand();
        this.typeReplaceUIManager = new TypeReplaceUIManager(this, typeReplaceCommand);

        this.commandRegistry = new CommandRegistry(this, gradientCommand, typeReplaceCommand,
                typeReplaceUIManager);
        this.commandRegistry.register();
    }

    @Override
    public void onEnable() {
        MessageManager.init(this);
        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null &&
                getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required for this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storageManager = new GradientStorageManager(this);
        storageManager.load();

        gradientCommand.setStorageManager(storageManager);

        typeReplaceUIManager.registerEvents();

        commandRegistry.registerWorldEditPatterns();

        commandRegistry.registerWorldEditPatterns();

        String[] logo = {
                "   ______ ______  __  __ _   _ ",
                "  / _____|___  / |  \\/  | \\ | |",
                " | |  __    / /  | \\  / |  \\| |",
                " | | |_ |  / /   | |\\/| | . ` |",
                " | |__| | / /__  | |  | | |\\  |",
                "  \\_____|/_____| |_|  |_|_| \\_|"
        };

        for (String line : logo) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + line);
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "GZMNBuildtools started up successfully!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.save();
        }
        getLogger().info("GZMNBuildtools disabled.");
    }

    public TypeReplaceUIManager getTypeReplaceUIManager() {
        return typeReplaceUIManager;
    }
}
