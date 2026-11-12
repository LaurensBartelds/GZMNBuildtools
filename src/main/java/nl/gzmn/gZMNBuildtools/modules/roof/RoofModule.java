package nl.gzmn.gZMNBuildtools.modules.roof;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.roof.command.RoofCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzroof: gable, hip, pyramid and flat roofs on a selection or the build you look at. */
@SuppressWarnings("UnstableApiUsage")
public final class RoofModule implements GzmnModule {

    private RoofCommand command;

    @Override
    public String id() {
        return "roof";
    }

    @Override
    public String description() {
        return "Gable, hip, pyramid and flat roofs on a selection or the looked-at build (/gzroof).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.BRICK_STAIRS, "/gzroof",
                        tr("Roof"),
                        tr("Put a gable, hip, pyramid or flat roof on your selection or the build you look at."))
                .example("/gzroof stone_brick_stairs").permission(RoofCommand.PERMISSION));
        command = new RoofCommand(context.plugin(), context.core().messages(),
                new BuildEditor(context.core().blocks()), context.core().previews(), context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzroof")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(RoofCommand.PERMISSION))
                .executes(ctx -> {
                    command.usage((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(sub("paint", RoofCommand.Mode.BUILD, false))
                .then(sub("preview", RoofCommand.Mode.BUILD, true))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.roof((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"),
                                    RoofCommand.Mode.SELECTION_OR_BUILD, false);
                            return 1;
                        }))
                .build(), "Put a gable, hip, pyramid or flat roof on a selection or the build you look at.",
                List.of("roof"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> sub(String name, RoofCommand.Mode mode, boolean preview) {
        return Commands.literal(name)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.roof((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"), mode, preview);
                            return 1;
                        }));
    }
}
