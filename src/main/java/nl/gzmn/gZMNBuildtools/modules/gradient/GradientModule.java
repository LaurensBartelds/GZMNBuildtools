package nl.gzmn.gZMNBuildtools.modules.gradient;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.WorldEdit;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.gradient.command.BracketBlocksArgumentType;
import nl.gzmn.gZMNBuildtools.modules.gradient.command.CommandSuggestions;
import nl.gzmn.gZMNBuildtools.modules.gradient.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.modules.gradient.pattern.GradientPatternParser;
import nl.gzmn.gZMNBuildtools.modules.gradient.service.GradientExecutor;
import nl.gzmn.gZMNBuildtools.modules.gradient.storage.GradientStorageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Selection gradients, saved gradients and the #gradient pattern (/gradient). */
@SuppressWarnings("UnstableApiUsage")
public final class GradientModule implements GzmnModule {

    private GradientCommand gradientCommand;
    private GradientStorageManager storage;
    private Messages messages;

    @Override
    public String id() {
        return "gradient";
    }

    @Override
    public String description() {
        return "Selection gradients, saved gradients and the #gradient pattern (/gradient).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.PAINTING, "/gradient",
                        tr("Gradients"),
                        tr("Fill your selection with a gradient between blocks, and save or share gradients."))
                .example("/gradient [stone][andesite][diorite] up").permission("gzmnbuildtools.gradient"));
        messages = context.core().messages();
        storage = new GradientStorageManager(context.plugin());
        storage.load();
        gradientCommand = new GradientCommand();
        gradientCommand.setStorageManager(storage);
        gradientCommand.setMessages(messages);
        gradientCommand.setPresetRegistry(context.core().presets());
        gradientCommand.setExecutor(new GradientExecutor());
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getPatternFactory().register(new GradientPatternParser(worldEdit, context.core().presets()));
        context.commands(this::registerGradient);
    }

    @Override
    public void disable() {
        if (storage != null) {
            storage.save();
        }
    }

    private void registerGradient(Commands commands) {
        commands.register(
                Commands.literal("gradient")
                        .requires(source -> source.getSender() instanceof Player &&
                                source.getSender().hasPermission(
                                        "gzmnbuildtools.gradient"))
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            messages.info(player,
                                    "Usage: /gradient <blocks> <direction> [mode]");
                            messages.info(player, "Single block layer: [stone]");
                            messages.info(player,
                                    "Multi-block layer: [cobblestone,stone]");
                            messages.info(player,
                                    "Example: /gradient [stone][cobblestone,stone] up blended");

                            // Show clickable block examples
                            messages.info(player,
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
                            messages.info(player,
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
                                    messages.info(player,
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

                                    messages.info(player,
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
}
