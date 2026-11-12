package nl.gzmn.gZMNBuildtools.modules.shadow;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.shadow.command.ShadowCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.WeAliases;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzshadow: cast a shadow of a selection or the looked-at build onto the surface around it. */
@SuppressWarnings("UnstableApiUsage")
public final class ShadowModule implements GzmnModule {

    private ShadowCommand command;

    @Override
    public String id() {
        return "shadow";
    }

    @Override
    public String description() {
        return "Cast a shadow of a selection or the looked-at build onto the surface around it (/gzshadow).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.BLACK_CARPET, "/gzshadow",
                        tr("Shadow"),
                        tr("Cast a shadow of the build you look at onto the surface around it."))
                .example("/gzshadow black_concrete").permission(ShadowCommand.PERMISSION));
        command = new ShadowCommand(context.plugin(), context.core().messages(),
                new BuildEditor(context.core().blocks()), context.core().previews(), context.core().limits());
        WeAliases.register(context.plugin(), "shadow", ShadowCommand.PERMISSION,
                (player, args) -> command.shadow(player, args, false));
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzshadow")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(ShadowCommand.PERMISSION))
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
                            command.shadow((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "args"), false);
                            return 1;
                        }))
                .build(), "Cast a shadow of a selection or the looked-at build onto the surface around it.",
                List.of("shadow"));
    }
}
