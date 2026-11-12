package nl.gzmn.gZMNBuildtools.modules.resize;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.resize.command.ResizeCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzresize: scale a cuboid selection up or down. */
@SuppressWarnings("UnstableApiUsage")
public final class ResizeModule implements GzmnModule {

    private ResizeCommand command;

    @Override
    public String id() {
        return "resize";
    }

    @Override
    public String description() {
        return "Scale a cuboid selection up or down (/gzresize).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.ITEM_FRAME, "/gzresize",
                        tr("Resize"),
                        tr("Scale your selection up or down per axis."))
                .example("/gzresize 2").permission(ResizeCommand.PERMISSION));
        command = new ResizeCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzresize")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(ResizeCommand.PERMISSION))
                .executes(ctx -> {
                    command.usage((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            command.resize((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"));
                            return 1;
                        }))
                .build(), "Scale a cuboid selection up or down.", List.of("resize"));
    }
}
