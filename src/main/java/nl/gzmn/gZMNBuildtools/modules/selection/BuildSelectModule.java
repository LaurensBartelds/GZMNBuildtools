package nl.gzmn.gZMNBuildtools.modules.selection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.selection.command.SelectCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import org.bukkit.entity.Player;

import java.util.List;

/** /gzselect: select the build you are looking at as a WorldEdit selection. */
@SuppressWarnings("UnstableApiUsage")
public final class BuildSelectModule implements GzmnModule {

    private SelectCommand selectCommand;

    @Override
    public String id() {
        return "build-select";
    }

    @Override
    public String description() {
        return "Select the build you are looking at as a WorldEdit selection (/gzselect).";
    }

    @Override
    public void enable(ModuleContext context) {
        selectCommand = new SelectCommand(context.plugin(), context.core().messages(),
                new BuildEditor(context.core().blocks()));
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzselect")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(SelectCommand.PERMISSION))
                .executes(ctx -> {
                    selectCommand.select((Player) ctx.getSource().getSender(), "build", 0);
                    return 1;
                })
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            SelectCommand.modes().stream()
                                    .filter(m -> m.startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            selectCommand.select((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "mode"), 0);
                            return 1;
                        })
                        .then(Commands.argument("padding", IntegerArgumentType.integer(0, 32))
                                .executes(ctx -> {
                                    selectCommand.select((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "mode"),
                                            IntegerArgumentType.getInteger(ctx, "padding"));
                                    return 1;
                                })))
                .build(), "Select the build you are looking at.", List.of("gsel"));
    }
}
