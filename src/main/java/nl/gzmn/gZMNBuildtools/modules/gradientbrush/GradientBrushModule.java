package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.command.GradientBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.menu.GradientBrushMenu;
import org.bukkit.entity.Player;

import java.util.List;

/** The smart, build-aware gradient brush and its menus (/gradientbrush, /gb). */
@SuppressWarnings("UnstableApiUsage")
public final class GradientBrushModule implements GzmnModule {

    private GradientBrushCommand brushCommand;
    private SmartGradientService service;
    private PresetRegistry presetRegistry;

    @Override
    public String id() {
        return "gradient-brush";
    }

    @Override
    public String description() {
        return "The smart, build-aware gradient brush and its menus (/gradientbrush, /gb).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        presetRegistry = context.core().presets();
        service = new SmartGradientService(presetRegistry);
        service.reload(context.plugin().getConfig());
        brushCommand = new GradientBrushCommand(context.plugin(), context.core().messages(), service,
                presetRegistry, context.core().previews());
        context.provide(SmartGradientService.class, service);
        context.commands(this::registerBrush);
    }

    @Override
    public void reload(ModuleContext context) {
        service.reload(context.plugin().getConfig());
    }

    private void registerBrush(Commands commands) {
        commands.register(Commands.literal("gradientbrush")
                .requires(source -> source.getSender().hasPermission(GradientBrushCommand.PERMISSION)
                        && source.getSender() instanceof Player)
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    new GradientBrushMenu(player, brushCommand, presetRegistry).open();
                    return 1;
                })
                .then(Commands.literal("status").executes(ctx -> {
                    brushCommand.status((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(gradientSub("set", brushCommand::set))
                .then(gradientSub("paint", brushCommand::paint))
                .then(gradientSub("preview", brushCommand::preview))
                .then(Commands.literal("info").executes(ctx -> {
                    brushCommand.info((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("suggest")
                        .executes(ctx -> {
                            brushCommand.suggest((Player) ctx.getSource().getSender(), null);
                            return 1;
                        })
                        .then(Commands.argument("block", StringArgumentType.word())
                                .executes(ctx -> {
                                    brushCommand.suggest((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "block"));
                                    return 1;
                                })))
                .then(Commands.literal("code")
                        .then(Commands.argument("code", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    brushCommand.code((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "code"));
                                    return 1;
                                })))
                .then(Commands.literal("web").executes(ctx -> {
                    brushCommand.web((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    brushCommand.reset((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("rescan").executes(ctx -> {
                    brushCommand.rescan((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.argument("palette", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            brushCommand.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            brushCommand.bind((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "palette"));
                            return 1;
                        }))
                .build(), "Smart, build-aware gradient brush. /gb <palette> [options] to bind it.",
                List.of("gb", "gbrush", "gzmnbrush"));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack>
    gradientSub(String name, java.util.function.BiConsumer<Player, String> action) {
        return Commands.literal(name)
                .executes(ctx -> {
                    action.accept((Player) ctx.getSource().getSender(), "");
                    return 1;
                })
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            brushCommand.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            action.accept((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"));
                            return 1;
                        }));
    }
}
