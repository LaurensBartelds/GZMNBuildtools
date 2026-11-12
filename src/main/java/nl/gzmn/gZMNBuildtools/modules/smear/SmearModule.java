package nl.gzmn.gZMNBuildtools.modules.smear;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.smear.command.SmearCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzsmear: a brush that smears blocks along the stroke direction. */
@SuppressWarnings("UnstableApiUsage")
public final class SmearModule implements GzmnModule {

    private SmearCommand command;

    @Override
    public String id() {
        return "smear";
    }

    @Override
    public String description() {
        return "Smear blocks along the stroke direction with a brush (/gzsmear).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BRUSHES, Material.CLAY_BALL, "/gzsmear",
                        tr("Smear"),
                        tr("Bind a brush that smears blocks along the stroke direction; negative amounts drag back."))
                .example("/gzsmear amount=3").permission(SmearCommand.PERMISSION));
        command = new SmearCommand(context.plugin(), context.core().messages(), context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzsmear")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(SmearCommand.PERMISSION))
                .executes(ctx -> {
                    command.bind((Player) ctx.getSource().getSender(), "");
                    return 1;
                })
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
                .build(), "Bind a brush that smears blocks along the stroke direction.", List.of("smear"));
    }
}
