package nl.gzmn.gZMNBuildtools.modules.fixconnect;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.fixconnect.command.FixConnectCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.WeAliases;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzfix: reconnect fences, walls and panes in the selection to their neighbours. */
@SuppressWarnings("UnstableApiUsage")
public final class FixConnectModule implements GzmnModule {

    private FixConnectCommand command;

    @Override
    public String id() {
        return "fix-connect";
    }

    @Override
    public String description() {
        return "Reconnect fences, walls and panes in the selection to their neighbours (/gzfix).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.OAK_FENCE, "/gzfix",
                        tr("Fix connections"),
                        tr("Reconnect fences, walls and panes in your selection to match their neighbours."))
                .example("/gzfix").permission(FixConnectCommand.PERMISSION));
        command = new FixConnectCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().limits(), context.core().blocks());
        WeAliases.register(context.plugin(), "fc", FixConnectCommand.PERMISSION, command::fix);
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzfix")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(FixConnectCommand.PERMISSION))
                .executes(ctx -> {
                    command.usage((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            command.fix((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"));
                            return 1;
                        }))
                .build(), "Reconnect fences, walls and panes in the selection to their neighbours.",
                List.of("fix", "fc"));
    }
}
