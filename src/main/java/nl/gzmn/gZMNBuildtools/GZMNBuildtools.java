package nl.gzmn.gZMNBuildtools;

import nl.gzmn.gZMNBuildtools.command.CommandRegistry;
import nl.gzmn.gZMNBuildtools.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.ui.typereplace.TypeReplaceUIManager;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
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

        getLogger().info("WorldEdit/FAWE detected, plugin enabled!");
        getLogger().info("Available commands:");
        getLogger().info(
                "  /typereplace <from> <to> - Replace block families (stairs, slabs, walls, fences, bars/grates)");
        getLogger().info("  /gradient                - Open gradient UI or use command syntax");
        getLogger().info("  /gradient save/use/list  - Save and reuse gradients");
        getLogger().info("  //set #gradient[direction][mode][blocks] - Use gradients with WorldEdit commands");
        getLogger().info("  Example: //set #gradient[up][linear][stone,andesite,deepslate]");
        getLogger().info("  Example: //set #gradient[down][smooth][50%stone,30%andesite,20%deepslate]");
        getLogger().info("Material groups: all_copper, all_waxed_copper, copper_all");
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
