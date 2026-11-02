package nl.gzmn.gZMNBuildtools.modules.generators;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.generators.command.GenerateCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Arches, cables, tori, cones, domes, spirals, trees and rocks (/gzgen). */
@SuppressWarnings("UnstableApiUsage")
public final class GeneratorsModule implements GzmnModule {

    private GenerateCommand generateCommand;

    @Override
    public String id() {
        return "generators";
    }

    @Override
    public String description() {
        return "Arches, cables, tori, cones, domes, spirals, trees and rocks (/gzgen).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        context.help(HelpEntry.of(HelpEntry.Category.SHAPES, Material.END_ROD, "/gzgen",
                        tr("Shape generators"),
                        tr("Arches, cables, domes, spirals, trees and rocks, built with any pattern."))
                .example("/gzgen dome stone_bricks").permission(GenerateCommand.PERMISSION));
        generateCommand = new GenerateCommand(context.plugin(), context.core().messages(), context.core().previews(),
                context.core().limits());
        context.commands(this::registerGenerate);
    }

    private void registerGenerate(Commands commands) {
        commands.register(Commands.literal("gzgen")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(GenerateCommand.PERMISSION))
                .executes(ctx -> {
                    generateCommand.list((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            generateCommand.typeSuggestions().stream()
                                    .filter(s -> s.startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            generateCommand.help((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "type"));
                            return 1;
                        })
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    generateCommand.argumentSuggestions(StringArgumentType.getString(ctx, "type"),
                                                    builder.getRemaining())
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    generateCommand.generate((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "type"),
                                            StringArgumentType.getString(ctx, "args"));
                                    return 1;
                                })))
                .build(), "Generate arches, cables, tori, cones, domes and spirals.", List.of("ggen"));
    }
}
