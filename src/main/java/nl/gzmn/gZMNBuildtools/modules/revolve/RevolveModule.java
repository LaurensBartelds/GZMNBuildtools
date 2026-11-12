package nl.gzmn.gZMNBuildtools.modules.revolve;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.revolve.command.RevolveCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzrevolve: revolve a selection around an axis through point 1. */
@SuppressWarnings("UnstableApiUsage")
public final class RevolveModule implements GzmnModule {

    private RevolveCommand command;

    @Override
    public String id() {
        return "revolve";
    }

    @Override
    public String description() {
        return "Revolve a selection around an axis through point 1 (/gzrevolve).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.SHAPES, Material.COMPASS, "/gzrevolve",
                        tr("Revolve"),
                        tr("Revolve your selection around an axis through point 1."))
                .example("/gzrevolve stone_bricks degrees=180").permission(RevolveCommand.PERMISSION));
        command = new RevolveCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzrevolve")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(RevolveCommand.PERMISSION))
                .executes(ctx -> {
                    command.usage((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.revolve((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"));
                            return 1;
                        }))
                .build(), "Revolve a selection around an axis through point 1.", List.of("revolve"));
    }
}
