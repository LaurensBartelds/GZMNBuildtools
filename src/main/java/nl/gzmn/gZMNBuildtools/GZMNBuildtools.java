package nl.gzmn.gZMNBuildtools;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nl.gzmn.gZMNBuildtools.commands.GradientCommand;
import nl.gzmn.gZMNBuildtools.commands.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.ui.GradientUIManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class GZMNBuildtools extends JavaPlugin {

    private GradientUIManager gradientUIManager;

    @Override
    public void onEnable() {
        // Check for WorldEdit/FAWE
        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null &&
            getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required for this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register UI event listeners now that plugin is enabled
        gradientUIManager.registerEvents();

        getLogger().info("WorldEdit/FAWE detected, plugin enabled!");
        getLogger().info("Available commands:");
        getLogger().info("  /typereplace <from> <to> - Replace block families (stairs, slabs, walls, fences, bars/grates)");
        getLogger().info("  /gradient                - Open gradient UI or use command syntax");
        getLogger().info("Material groups: all_copper, all_waxed_copper, copper_all");
    }

    public GZMNBuildtools() {
        // Initialize UI manager and commands
        this.gradientUIManager = new GradientUIManager(this, null);
        GradientCommand gradientCommand = new GradientCommand(gradientUIManager);
        TypeReplaceCommand typeReplaceCommand = new TypeReplaceCommand();

        // Register commands using Paper's Lifecycle Events API
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            // Register /typereplace command
            commands.register(
                Commands.literal("typereplace")
                    .requires(source -> source.getSender() instanceof Player &&
                              source.getSender().hasPermission("gzmnbuildtools.typereplace"))
                    .then(Commands.argument("from_material", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            typeReplaceCommand.getSuggestions().stream()
                                .filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("to_material", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                typeReplaceCommand.getSuggestions().stream()
                                    .filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String from = StringArgumentType.getString(ctx, "from_material");
                                String to = StringArgumentType.getString(ctx, "to_material");
                                typeReplaceCommand.execute((Player) ctx.getSource().getSender(), from, to);
                                return 1;
                            })
                        )
                    )
                    .build(),
                "Replace entire block type families (stairs, slabs, walls, etc.) preserving variants.",
                List.of("tr", "typerep")
            );

            // Register /gradient command
            commands.register(
                Commands.literal("gradient")
                    .requires(source -> source.getSender() instanceof Player &&
                              source.getSender().hasPermission("gzmnbuildtools.gradient"))
                    .executes(ctx -> {
                        gradientCommand.openUI((Player) ctx.getSource().getSender());
                        return 1;
                    })
                    .then(Commands.argument("blocks", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            gradientCommand.getBlockSuggestions().stream()
                                .filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("direction", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                gradientCommand.getDirectionSuggestions().stream()
                                    .filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    gradientCommand.getModeSuggestions().stream()
                                        .filter(s -> s.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                        .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String blocks = StringArgumentType.getString(ctx, "blocks");
                                    String direction = StringArgumentType.getString(ctx, "direction");
                                    String mode = StringArgumentType.getString(ctx, "mode");
                                    gradientCommand.execute((Player) ctx.getSource().getSender(), blocks, direction, mode);
                                    return 1;
                                })
                            )
                            .executes(ctx -> {
                                String blocks = StringArgumentType.getString(ctx, "blocks");
                                String direction = StringArgumentType.getString(ctx, "direction");
                                gradientCommand.execute((Player) ctx.getSource().getSender(), blocks, direction, "LINEAR");
                                return 1;
                            })
                        )
                    )
                    .build(),
                "Apply or create gradients with a visual UI.",
                List.of("grad", "grd")
            );
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("GZMNBuildtools disabled.");
    }

    public GradientUIManager getGradientUIManager() {
        return gradientUIManager;
    }
}
