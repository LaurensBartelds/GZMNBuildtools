package nl.gzmn.gZMNBuildtools;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.WorldEdit;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nl.gzmn.gZMNBuildtools.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.gradient.service.BlockColorService;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.ui.gradient.GradientUIManager;
import nl.gzmn.gZMNBuildtools.ui.typereplace.TypeReplaceUIManager;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import nl.gzmn.gZMNBuildtools.integration.worldedit.GradientPatternParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class GZMNBuildtools extends JavaPlugin {

        private GradientUIManager gradientUIManager;
        private TypeReplaceUIManager typeReplaceUIManager;
        private GradientStorageManager storageManager;
        private GradientCommand gradientCommand;

        public GZMNBuildtools() {
                this.gradientUIManager = new GradientUIManager(this, null);

                this.gradientCommand = new GradientCommand(gradientUIManager);
                TypeReplaceCommand typeReplaceCommand = new TypeReplaceCommand();
                this.typeReplaceUIManager = new TypeReplaceUIManager(this, typeReplaceCommand);

                getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                        Commands commands = event.registrar();

                        commands.register(
                                        Commands.literal("typereplace")
                                                        .requires(source -> source.getSender() instanceof Player &&
                                                                        source.getSender().hasPermission(
                                                                                        "gzmnbuildtools.typereplace"))
                                                        .executes(ctx -> {
                                                                typeReplaceUIManager.openTypeReplaceUI(
                                                                                (Player) ctx.getSource().getSender());
                                                                return 1;
                                                        })
                                                        .then(Commands.argument("from_material",
                                                                        StringArgumentType.word())
                                                                        .suggests((ctx, builder) -> {
                                                                                typeReplaceCommand.getSuggestions()
                                                                                                .stream()
                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                .startsWith(builder
                                                                                                                                .getRemainingLowerCase()))
                                                                                                .forEach(builder::suggest);
                                                                                return builder.buildFuture();
                                                                        })
                                                                        .then(Commands.argument("to_material",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                typeReplaceCommand
                                                                                                                .getSuggestions()
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                                String from = StringArgumentType
                                                                                                                .getString(ctx, "from_material");
                                                                                                String to = StringArgumentType
                                                                                                                .getString(ctx, "to_material");
                                                                                                typeReplaceCommand
                                                                                                                .execute((Player) ctx
                                                                                                                                .getSource()
                                                                                                                                .getSender(),
                                                                                                                                from,
                                                                                                                                to);
                                                                                                return 1;
                                                                                        })))
                                                        .build(),
                                        "Replace entire block type families (stairs, slabs, walls, etc.) preserving variants.",
                                        List.of("tr", "typerep"));

                        commands.register(
                                        Commands.literal("gradient")
                                                        .requires(source -> source.getSender() instanceof Player &&
                                                                        source.getSender().hasPermission(
                                                                                        "gzmnbuildtools.gradient"))
                                                        .executes(ctx -> {
                                                                gradientCommand.openUI(
                                                                                (Player) ctx.getSource().getSender());
                                                                return 1;
                                                        })
                                                        .then(Commands.literal("easy")
                                                                        .executes(ctx -> {
                                                                                gradientCommand.openEasyMode(
                                                                                                (Player) ctx.getSource()
                                                                                                                .getSender());
                                                                                return 1;
                                                                        }))
                                                        .then(Commands.literal("advanced")
                                                                        .executes(ctx -> {
                                                                                gradientCommand.openAdvancedMode(
                                                                                                (Player) ctx.getSource()
                                                                                                                .getSender());
                                                                                return 1;
                                                                        }))
                                                        
                                                        .then(Commands.literal("save")
                                                                        .then(Commands.argument("name",
                                                                                        StringArgumentType.word())
                                                                                        .executes(ctx -> {
                                                                                                gradientCommand.saveGradient(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender(),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "name"),
                                                                                                                false);
                                                                                                return 1;
                                                                                        })
                                                                                        .then(Commands.literal("public")
                                                                                                        .executes(ctx -> {
                                                                                                                gradientCommand.saveGradient(
                                                                                                                                (Player) ctx.getSource()
                                                                                                                                                .getSender(),
                                                                                                                                StringArgumentType
                                                                                                                                                .getString(ctx, "name"),
                                                                                                                                true);
                                                                                                                return 1;
                                                                                                        }))))
                                                        
                                                        .then(Commands.literal("use")
                                                                        .then(Commands.argument("name",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                gradientCommand.getSavedGradientSuggestions(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender())
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .then(Commands.argument(
                                                                                                        "direction",
                                                                                                        StringArgumentType
                                                                                                                        .word())
                                                                                                        .suggests((ctx, builder) -> {
                                                                                                                gradientCommand.getDirectionSuggestions()
                                                                                                                                .stream()
                                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                                .startsWith(builder
                                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                                .forEach(builder::suggest);
                                                                                                                return builder.buildFuture();
                                                                                                        })
                                                                                                        .executes(ctx -> {
                                                                                                                gradientCommand.useGradient(
                                                                                                                                (Player) ctx.getSource()
                                                                                                                                                .getSender(),
                                                                                                                                StringArgumentType
                                                                                                                                                .getString(ctx, "name"),
                                                                                                                                StringArgumentType
                                                                                                                                                .getString(ctx, "direction"));
                                                                                                                return 1;
                                                                                                        }))))
                                                        
                                                        .then(Commands.literal("list")
                                                                        .executes(ctx -> {
                                                                                gradientCommand.listGradients(
                                                                                                (Player) ctx.getSource()
                                                                                                                .getSender(),
                                                                                                false);
                                                                                return 1;
                                                                        })
                                                                        .then(Commands.literal("global")
                                                                                        .executes(ctx -> {
                                                                                                gradientCommand.listGradients(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender(),
                                                                                                                true);
                                                                                                return 1;
                                                                                        })))
                                                        
                                                        .then(Commands.literal("delete")
                                                                        .then(Commands.argument("name",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                gradientCommand.getSavedGradientSuggestions(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender())
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                                gradientCommand.deleteGradient(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender(),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "name"));
                                                                                                return 1;
                                                                                        })))
                                                        
                                                        .then(Commands.literal("share")
                                                                        .then(Commands.argument("name",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                gradientCommand.getSavedGradientSuggestions(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender())
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                                gradientCommand.shareGradient(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender(),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "name"));
                                                                                                return 1;
                                                                                        })))
                                                        .then(Commands.literal("preset")
                                                                        .then(Commands.argument("preset_id",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                gradientCommand.getPresetSuggestions()
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .then(Commands.argument(
                                                                                                        "direction",
                                                                                                        StringArgumentType
                                                                                                                        .word())
                                                                                                        .suggests((ctx, builder) -> {
                                                                                                                gradientCommand.getDirectionSuggestions()
                                                                                                                                .stream()
                                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                                .startsWith(builder
                                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                                .forEach(builder::suggest);
                                                                                                                return builder.buildFuture();
                                                                                                        })
                                                                                                        .executes(ctx -> {
                                                                                                                String presetId = StringArgumentType
                                                                                                                                .getString(ctx, "preset_id");
                                                                                                                String direction = StringArgumentType
                                                                                                                                .getString(ctx, "direction");
                                                                                                                gradientCommand.executePreset(
                                                                                                                                (Player) ctx.getSource()
                                                                                                                                                .getSender(),
                                                                                                                                presetId,
                                                                                                                                direction);
                                                                                                                return 1;
                                                                                                        }))))
                                                        .then(Commands.argument("blocks", StringArgumentType.word())
                                                                        .suggests((ctx, builder) -> {
                                                                                gradientCommand.getBlockSuggestions()
                                                                                                .stream()
                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                .startsWith(builder
                                                                                                                                .getRemainingLowerCase()))
                                                                                                .forEach(builder::suggest);
                                                                                return builder.buildFuture();
                                                                        })
                                                                        .then(Commands.argument("direction",
                                                                                        StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                                gradientCommand.getDirectionSuggestions()
                                                                                                                .stream()
                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                .startsWith(builder
                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                .forEach(builder::suggest);
                                                                                                return builder.buildFuture();
                                                                                        })
                                                                                        .then(Commands.argument("mode",
                                                                                                        StringArgumentType
                                                                                                                        .word())
                                                                                                        .suggests((ctx, builder) -> {
                                                                                                                gradientCommand.getModeSuggestions()
                                                                                                                                .stream()
                                                                                                                                .filter(s -> s.toLowerCase()
                                                                                                                                                .startsWith(builder
                                                                                                                                                                .getRemainingLowerCase()))
                                                                                                                                .forEach(builder::suggest);
                                                                                                                return builder.buildFuture();
                                                                                                        })
                                                                                                        .executes(ctx -> {
                                                                                                                String blocks = StringArgumentType
                                                                                                                                .getString(ctx, "blocks");
                                                                                                                String direction = StringArgumentType
                                                                                                                                .getString(ctx,
                                                                                                                                                "direction");
                                                                                                                String mode = StringArgumentType
                                                                                                                                .getString(ctx, "mode");
                                                                                                                gradientCommand.execute(
                                                                                                                                (Player) ctx.getSource()
                                                                                                                                                .getSender(),
                                                                                                                                blocks,
                                                                                                                                direction,
                                                                                                                                mode);
                                                                                                                return 1;
                                                                                                        }))
                                                                                        .executes(ctx -> {
                                                                                                String blocks = StringArgumentType
                                                                                                                .getString(ctx, "blocks");
                                                                                                String direction = StringArgumentType
                                                                                                                .getString(ctx, "direction");
                                                                                                gradientCommand.execute(
                                                                                                                (Player) ctx.getSource()
                                                                                                                                .getSender(),
                                                                                                                blocks,
                                                                                                                direction,
                                                                                                                "LINEAR");
                                                                                                return 1;
                                                                                        })))
                                                        .build(),
                                        "Apply or create gradients with a visual UI. Use /gradient easy for presets or /gradient advanced for full control.",
                                        List.of("grad", "grd"));

                        commands.register(
                                        Commands.literal("gzmndebug")
                                                        .requires(source -> source.getSender()
                                                                        .hasPermission("gzmnbuildtools.admin"))
                                                        .executes(ctx -> {
                                                                getLogger().info(
                                                                                "Manual registration triggered via /gzmndebug");
                                                                registerWorldEditPatterns();
                                                                ctx.getSource().getSender().sendMessage(
                                                                                net.kyori.adventure.text.Component.text(
                                                                                                "Attempted to re-register patterns. Check console."));
                                                                return 1;
                                                        })
                                                        .build(),
                                        "Debug GZMNBuildtools",
                                        List.of());
                });
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

                BlockColorService blockColorService = new BlockColorService(
                                this);

                
                gradientUIManager.setStorageManager(storageManager);
                gradientUIManager.setBlockColorService(blockColorService);
                gradientCommand.setStorageManager(storageManager);

                gradientUIManager.registerEvents();
                typeReplaceUIManager.registerEvents();

                
                registerWorldEditPatterns();

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

        
        private void registerWorldEditPatterns() {
                try {
                        WorldEdit worldEdit = WorldEdit.getInstance();
                        worldEdit.getPatternFactory().register(new GradientPatternParser(worldEdit, getLogger()));
                        getLogger().info("Registered #gradient pattern with WorldEdit");
                } catch (Throwable e) {
                        getLogger().warning("Failed to register WorldEdit patterns: " + e.getMessage());
                        e.printStackTrace();
                        getLogger().warning("Gradient pattern syntax will not be available");
                }
        }

        @Override
        public void onDisable() {
                if (storageManager != null) {
                        storageManager.save();
                }
                getLogger().info("GZMNBuildtools disabled.");
        }

        public GradientUIManager getGradientUIManager() {
                return gradientUIManager;
        }

        public TypeReplaceUIManager getTypeReplaceUIManager() {
                return typeReplaceUIManager;
        }
}
