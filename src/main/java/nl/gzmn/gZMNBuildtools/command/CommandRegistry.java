package nl.gzmn.gZMNBuildtools.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.WorldEdit;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nl.gzmn.gZMNBuildtools.GZMNBuildtools;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import nl.gzmn.gZMNBuildtools.integration.worldedit.GradientPatternParser;
import nl.gzmn.gZMNBuildtools.ui.typereplace.TypeReplaceUIManager;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CommandRegistry {

    private final GZMNBuildtools plugin;
    private final GradientCommand gradientCommand;
    private final TypeReplaceCommand typeReplaceCommand;
    private final TypeReplaceUIManager typeReplaceUIManager;

    public CommandRegistry(GZMNBuildtools plugin, GradientCommand gradientCommand,
                           TypeReplaceCommand typeReplaceCommand, TypeReplaceUIManager typeReplaceUIManager) {
        this.plugin = plugin;
        this.gradientCommand = gradientCommand;
        this.typeReplaceCommand = typeReplaceCommand;
        this.typeReplaceUIManager = typeReplaceUIManager;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            registerTypeReplace(commands);
            registerGradient(commands);
            registerDebug(commands);
        });
    }

    private void registerTypeReplace(Commands commands) {
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
                        .then(Commands.argument("from_material", StringArgumentType.word())
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
                                            typeReplaceCommand.execute(
                                                    (Player) ctx.getSource()
                                                            .getSender(),
                                                    from, to);
                                            return 1;
                                        })))
                        .build(),
                "Replace entire block type families (stairs, slabs, walls, etc.) preserving variants.",
                List.of("tr", "typerep"));
    }

    private void registerGradient(Commands commands) {
        commands.register(
                Commands.literal("gradient")
                        .requires(source -> source.getSender() instanceof Player &&
                                source.getSender().hasPermission(
                                        "gzmnbuildtools.gradient"))
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            MessageManager.info(player,
                                    "Usage: /gradient <blocks> <direction> [mode]");
                            MessageManager.info(player, "Single block layer: [stone]");
                            MessageManager.info(player,
                                    "Multi-block layer: [cobblestone,stone]");
                            MessageManager.info(player,
                                    "Example: /gradient [stone][cobblestone,stone] up blended");

                            // Show clickable block examples
                            MessageManager.info(player,
                                    "Click to start (then add more blocks):");
                            String[] examples = {"stone", "dirt", "cobblestone",
                                    "andesite", "deepslate"};
                            for (String block : examples) {
                                player.sendMessage(
                                        net.kyori.adventure.text.Component
                                                .text("  [" + block
                                                        + "]")
                                                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                                                .clickEvent(net.kyori.adventure.text.event.ClickEvent
                                                        .suggestCommand("/gradient ["
                                                                + block
                                                                + "]"))
                                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                                                        .showText(net.kyori.adventure.text.Component
                                                                .text("Click to use "
                                                                        + block))));
                            }

                            // Show subcommands
                            MessageManager.info(player,
                                    "Subcommands: save, use, list, delete, share, preset");
                            return 1;
                        })
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
                                            gradientCommand
                                                    .getSavedGradientSuggestions(
                                                            (Player) ctx.getSource()
                                                                    .getSender())
                                                    .stream()
                                                    .filter(s -> s.toLowerCase()
                                                            .startsWith(builder
                                                                    .getRemainingLowerCase()))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("direction",
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
                                            gradientCommand
                                                    .getSavedGradientSuggestions(
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
                                            gradientCommand
                                                    .getSavedGradientSuggestions(
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
                                        .then(Commands.argument("direction",
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
                                                    gradientCommand.executePreset(
                                                            (Player) ctx.getSource()
                                                                    .getSender(),
                                                            StringArgumentType
                                                                    .getString(ctx, "preset_id"),
                                                            StringArgumentType
                                                                    .getString(ctx, "direction"));
                                                    return 1;
                                                }))))
                        .then(Commands.argument("blocks",
                                        BracketBlocksArgumentType.bracketBlocks())
                                .suggests((ctx, builder) -> CommandSuggestions
                                        .suggestBlocks(builder))
                                .executes(ctx -> {
                                    // User provided blocks but no direction
                                    Player player = (Player) ctx.getSource()
                                            .getSender();
                                    String blocks = BracketBlocksArgumentType
                                            .getString(ctx, "blocks");
                                    MessageManager.info(player,
                                            "Please specify a direction (click to use):");

                                    for (String dir : gradientCommand
                                            .getDirectionSuggestions()) {
                                        player.sendMessage(
                                                net.kyori.adventure.text.Component
                                                        .text("  [" + dir
                                                                + "]")
                                                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                                                        .clickEvent(net.kyori.adventure.text.event.ClickEvent
                                                                .suggestCommand(
                                                                        "/gradient " + blocks
                                                                                + " "
                                                                                + dir
                                                                                + " "))
                                                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                                                                .showText(net.kyori.adventure.text.Component
                                                                        .text("Click to use "
                                                                                + dir))));
                                    }

                                    MessageManager.info(player,
                                            "Optional modes (click to add):");
                                    for (String mode : gradientCommand
                                            .getModeSuggestions()) {
                                        player.sendMessage(
                                                net.kyori.adventure.text.Component
                                                        .text("  [" + mode
                                                                + "]")
                                                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                                                        .clickEvent(net.kyori.adventure.text.event.ClickEvent
                                                                .suggestCommand(
                                                                        "/gradient " + blocks
                                                                                + " VERTICAL_UP "
                                                                                + mode))
                                                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                                                                .showText(net.kyori.adventure.text.Component
                                                                        .text("Click to use "
                                                                                + mode
                                                                                + " mode"))));
                                    }
                                    return 1;
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
                                                            .getString(ctx, "direction");
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
                "Apply or create gradients. Use /gradient <blocks> <direction> [mode]",
                List.of("grad", "grd"));
    }

    private void registerDebug(Commands commands) {
        commands.register(Commands.literal("gzmndebug")
                .requires(source -> source.getSender().hasPermission("gzmnbuildtools.admin"))
                .executes(ctx -> {
                    plugin.getLogger().info("Manual registration triggered via /gzmndebug");
                    registerWorldEditPatterns();
                    ctx.getSource().getSender().sendMessage(
                            net.kyori.adventure.text.Component
                                    .text("Attempted to re-register patterns. Check console."));
                    return 1;
                }).build(), "Debug GZMNBuildtools", List.of());
    }

    public void registerWorldEditPatterns() {
        try {
            WorldEdit worldEdit = WorldEdit.getInstance();
            worldEdit.getPatternFactory().register(new GradientPatternParser(worldEdit));
            plugin.getLogger().info("Registered #gradient pattern with WorldEdit");
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to register WorldEdit patterns: " + e.getMessage());
            e.printStackTrace();
            plugin.getLogger().warning("Gradient pattern syntax will not be available");
        }
    }
}
