package nl.gzmn.gZMNBuildtools.modules.materialswap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.materialswap.command.SwapCommand;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** /gzswap: swap a build's material while keeping shapes, states and textures. */
@SuppressWarnings("UnstableApiUsage")
public final class MaterialSwapModule implements GzmnModule {

    private SwapCommand swapCommand;

    @Override
    public String id() {
        return "material-swap";
    }

    @Override
    public String description() {
        return "Swap a build's material, keeping stairs, slabs, walls and mossy/cracked forms (/gzswap).";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BRUSHES, Material.DEEPSLATE_BRICKS, "/gzswap",
                        tr("Material swap"),
                        tr("Swap a build to another material with a brush or in one go, keeping shapes and mossy or "
                                + "cracked forms."))
                .example("/gzswap deepslate_bricks").permission(SwapCommand.PERMISSION));
        swapCommand = new SwapCommand(context.plugin(), context.core().messages(),
                new BuildEditor(context.core().blocks()), context.core().previews(),
                context.core().limits());
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzswap")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(SwapCommand.PERMISSION))
                .executes(ctx -> {
                    swapCommand.help((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(sub("paint", false))
                .then(sub("preview", true))
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            swapCommand.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            swapCommand.bind((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"));
                            return 1;
                        }))
                .build(), "Swap a build's material while keeping its shapes.", List.of("swap"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> sub(String name, boolean preview) {
        return Commands.literal(name)
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            swapCommand.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            swapCommand.paint((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"), preview);
                            return 1;
                        }));
    }
}
