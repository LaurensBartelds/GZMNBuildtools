package nl.gzmn.gZMNBuildtools.modules.splines;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.splines.command.SplineCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Tubes, walls and roads along a curve through picked points (/gzspline). */
@SuppressWarnings("UnstableApiUsage")
public final class SplinesModule implements GzmnModule {

    private SplineCommand splineCommand;

    @Override
    public String id() {
        return "splines";
    }

    @Override
    public String description() {
        return "Tubes, walls and roads along a curve through picked points (/gzspline).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        context.help(HelpEntry.of(HelpEntry.Category.SHAPES, Material.STRING, "/gzspline",
                        tr("Splines and rivers"),
                        tr("Tubes, walls, roads and rivers along a smooth curve through points you pick."))
                .example("/gzspline build river gravel").permission(SplineCommand.PERMISSION).runs());
        splineCommand = new SplineCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().blocks(), context.core().limits());
        context.commands(this::registerSpline);
    }

    private void registerSpline(Commands commands) {
        commands.register(Commands.literal("gzspline")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(SplineCommand.PERMISSION))
                .executes(ctx -> {
                    splineCommand.help((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("add").executes(ctx -> {
                    splineCommand.add((Player) ctx.getSource().getSender(), false);
                    return 1;
                }))
                .then(Commands.literal("here").executes(ctx -> {
                    splineCommand.add((Player) ctx.getSource().getSender(), true);
                    return 1;
                }))
                .then(Commands.literal("undo").executes(ctx -> {
                    splineCommand.undo((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    splineCommand.clear((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    splineCommand.list((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("show").executes(ctx -> {
                    splineCommand.show((Player) ctx.getSource().getSender(), 10);
                    return 1;
                }))
                .then(Commands.literal("build")
                        .then(Commands.argument("shape", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String s : List.of("tube", "wall", "road", "river")) {
                                        if (s.startsWith(builder.getRemainingLowerCase())) {
                                            builder.suggest(s);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("args", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            splineCommand.argumentSuggestions(StringArgumentType.getString(ctx, "shape"),
                                                    builder.getRemaining()).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            splineCommand.build((Player) ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "shape"),
                                                    StringArgumentType.getString(ctx, "args"));
                                            return 1;
                                        }))))
                .build(), "Build tubes, walls, roads and rivers along a curve through points you pick.", List.of("gsp"));
    }
}
