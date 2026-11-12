package nl.gzmn.gZMNBuildtools.modules.loft;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.loft.command.LoftCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzloft: loft a smooth organic surface through frames you place. */
@SuppressWarnings("UnstableApiUsage")
public final class LoftModule implements GzmnModule {

    private LoftCommand command;

    @Override
    public String id() {
        return "loft";
    }

    @Override
    public String description() {
        return "Loft a smooth organic surface through frames you place (/gzloft).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.SHAPES, Material.PAPER, "/gzloft",
                        tr("Loft"),
                        tr("Loft a smooth organic surface through frames you place."))
                .example("/gzloft build oak_planks").permission(LoftCommand.PERMISSION).runs());
        command = new LoftCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzloft")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(LoftCommand.PERMISSION))
                .executes(ctx -> {
                    command.help((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(subWithSize("add"))
                .then(subWithSize("here"))
                .then(Commands.literal("frame")
                        .then(Commands.argument("size", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    command.frame((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "size"));
                                    return 1;
                                })))
                .then(Commands.literal("remove").executes(ctx -> {
                    command.remove((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    command.clear((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    command.list((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("show").executes(ctx -> {
                    command.show((Player) ctx.getSource().getSender(), 10);
                    return 1;
                }))
                .then(Commands.literal("build")
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    command.build((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "args"));
                                    return 1;
                                })))
                .build(), "Loft a smooth organic surface through frames you place.", List.of("loft"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> subWithSize(String name) {
        return Commands.literal(name)
                .executes(ctx -> {
                    command.add((Player) ctx.getSource().getSender(), name.equals("here"), null);
                    return 1;
                })
                .then(Commands.argument("size", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            command.add((Player) ctx.getSource().getSender(), name.equals("here"),
                                    StringArgumentType.getString(ctx, "size"));
                            return 1;
                        }));
    }
}
