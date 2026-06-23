package nl.gzmn.gZMNBuildtools;

import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.command.CommandRegistry;
import nl.gzmn.gZMNBuildtools.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.core.PluginContext;
import nl.gzmn.gZMNBuildtools.ui.typereplace.TypeReplaceUIManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class GZMNBuildtools extends JavaPlugin {

    private PluginContext context;
    private TypeReplaceUIManager typeReplaceUIManager;
    private GradientCommand gradientCommand;
    private CommandRegistry commandRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null &&
                getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required for this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Composition root: construct and load all services (messages, registries,
        // executor, storage) in one place.
        context = new PluginContext(this);
        context.load();

        // Construct commands/managers, wiring their dependencies from the context
        // up front so no command can ever execute against a half-initialised plugin.
        gradientCommand = new GradientCommand();
        gradientCommand.setStorageManager(context.storage());
        gradientCommand.setMessages(context.messages());
        gradientCommand.setPresetRegistry(context.presets());
        gradientCommand.setExecutor(context.executor());

        TypeReplaceCommand typeReplaceCommand = new TypeReplaceCommand();
        typeReplaceCommand.setMessages(context.messages());
        typeReplaceCommand.setBlockFamilies(context.blockFamilies());

        typeReplaceUIManager = new TypeReplaceUIManager(this, typeReplaceCommand);
        typeReplaceUIManager.setMessages(context.messages());
        typeReplaceUIManager.setBlockFamilies(context.blockFamilies());

        commandRegistry = new CommandRegistry(this, gradientCommand, typeReplaceCommand,
                typeReplaceUIManager, context.messages(), context.presets(), context.blockFamilies());
        commandRegistry.register();

        typeReplaceUIManager.registerEvents();
        commandRegistry.registerWorldEditPatterns();

        // Expose the public extension API so other plugins can register presets / families.
        getServer().getServicesManager().register(GzmnBuildtoolsApi.class, context, this, ServicePriority.Normal);

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
        if (context != null) {
            context.storage().save();
        }
        getLogger().info("GZMNBuildtools disabled.");
    }

    public TypeReplaceUIManager getTypeReplaceUIManager() {
        return typeReplaceUIManager;
    }
}
