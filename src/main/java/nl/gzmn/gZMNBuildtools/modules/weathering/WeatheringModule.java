package nl.gzmn.gZMNBuildtools.modules.weathering;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.weathering.command.WeatheringCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import org.bukkit.entity.Player;

import java.util.List;

/** /weathering: age builds with moss, cracks, worn edges and vines. */
@SuppressWarnings("UnstableApiUsage")
public final class WeatheringModule implements GzmnModule {

    private WeatheringCommand command;

    @Override
    public String id() {
        return "weathering";
    }

    @Override
    public String description() {
        return "Age builds with moss, cracks, worn edges and vines (/weathering, /age).";
    }

    @Override
    public void enable(ModuleContext context) {
        command = new WeatheringCommand(context.plugin(), context.core().messages(),
                new BuildEditor(context.core().blocks()), context.core().previews(),
                context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("weathering")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(WeatheringCommand.PERMISSION))
                .executes(ctx -> {
                    command.bind((Player) ctx.getSource().getSender(), "");
                    return 1;
                })
                .then(sub("paint", false))
                .then(sub("preview", true))
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.bind((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"));
                            return 1;
                        }))
                .build(), "Age a build with moss, cracks, worn edges and vines.", List.of("age"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> sub(String name, boolean preview) {
        return Commands.literal(name)
                .executes(ctx -> {
                    command.paint((Player) ctx.getSource().getSender(), "", preview);
                    return 1;
                })
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.paint((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"), preview);
                            return 1;
                        }));
    }
}
