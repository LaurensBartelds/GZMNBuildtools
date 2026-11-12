package nl.gzmn.gZMNBuildtools;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.core.CoreCommands;
import nl.gzmn.gZMNBuildtools.core.PluginContext;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.help.HelpHub;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleHost;
import nl.gzmn.gZMNBuildtools.core.module.ModuleManager;
import nl.gzmn.gZMNBuildtools.modules.fixconnect.FixConnectModule;
import nl.gzmn.gZMNBuildtools.modules.generators.GeneratorsModule;
import nl.gzmn.gZMNBuildtools.modules.gradient.GradientModule;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.GradientBrushModule;
import nl.gzmn.gZMNBuildtools.modules.hover.HoverPreviewModule;
import nl.gzmn.gZMNBuildtools.modules.loft.LoftModule;
import nl.gzmn.gZMNBuildtools.modules.materialswap.MaterialSwapModule;
import nl.gzmn.gZMNBuildtools.modules.patterns.PatternsModule;
import nl.gzmn.gZMNBuildtools.modules.pixelart.PixelArtModule;
import nl.gzmn.gZMNBuildtools.modules.resize.ResizeModule;
import nl.gzmn.gZMNBuildtools.modules.revolve.RevolveModule;
import nl.gzmn.gZMNBuildtools.modules.roof.RoofModule;
import nl.gzmn.gZMNBuildtools.modules.scatter.PrefabScatterModule;
import nl.gzmn.gZMNBuildtools.modules.shadow.ShadowModule;
import nl.gzmn.gZMNBuildtools.modules.selection.BuildSelectModule;
import nl.gzmn.gZMNBuildtools.modules.smear.SmearModule;
import nl.gzmn.gZMNBuildtools.modules.splines.SplinesModule;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.TerrainBrushModule;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.TerrainTextureModule;
import nl.gzmn.gZMNBuildtools.modules.typereplace.TypeReplaceModule;
import nl.gzmn.gZMNBuildtools.modules.weathering.WeatheringModule;
import nl.gzmn.gZMNBuildtools.modules.webeditor.WebEditorModule;
import nl.gzmn.gZMNBuildtools.shared.menu.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bootstrap: starts the core services, then the feature modules (each can be
 * switched off under {@code modules:} in config.yml).
 */
@SuppressWarnings("UnstableApiUsage")
public final class GZMNBuildtools extends JavaPlugin {

    private ModuleManager modules;
    private PluginContext context;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null &&
                getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required for this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        context = new PluginContext(this);
        context.load();
        context.limits().start(this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        modules = new ModuleManager(host(context));
        allModules().forEach(modules::add);
        modules.enableAll();

        CoreCommands coreCommands = new CoreCommands(this, context, modules);
        HelpHub help = new HelpHub(context.messages(), () -> {
            List<HelpEntry> all = new ArrayList<>(modules.helpEntries());
            all.addAll(CoreCommands.help());
            return all;
        });
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            coreCommands.register(event.registrar());
            help.register(event.registrar());
            modules.commandRegistrars().forEach(registrar -> registrar.accept(event.registrar()));
        });

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
        getLogger().info("Modules: " + CoreCommands.summary(modules));
    }

    /** Every feature module; the order only matters for display, dependencies decide the start order. */
    public static List<GzmnModule> allModules() {
        return List.of(
                new PatternsModule(),
                new GradientModule(),
                new TypeReplaceModule(),
                new GradientBrushModule(),
                new TerrainBrushModule(),
                new GeneratorsModule(),
                new SplinesModule(),
                new WebEditorModule(),
                new BuildSelectModule(),
                new MaterialSwapModule(),
                new WeatheringModule(),
                new PixelArtModule(),
                new TerrainTextureModule(),
                new HoverPreviewModule(),
                new PrefabScatterModule(),
                new RoofModule(),
                new LoftModule(),
                new RevolveModule(),
                new ResizeModule(),
                new SmearModule(),
                new ShadowModule(),
                new FixConnectModule());
    }

    private ModuleHost host(PluginContext context) {
        JavaPlugin plugin = this;
        return new ModuleHost() {
            @Override
            public JavaPlugin plugin() {
                return plugin;
            }

            @Override
            public PluginContext core() {
                return context;
            }

            @Override
            public Logger logger() {
                return getLogger();
            }

            @Override
            public boolean enabledInConfig(String moduleId) {
                return getConfig().getBoolean("modules." + moduleId, true);
            }

            @Override
            public void registerListener(Listener listener) {
                getServer().getPluginManager().registerEvents(listener, plugin);
            }
        };
    }

    @Override
    public void onDisable() {
        if (modules != null) {
            modules.disableAll();
        }
        if (context != null) {
            context.limits().stop();
            context.playerData().flush();
        }
        getLogger().info("GZMNBuildtools disabled.");
    }
}
