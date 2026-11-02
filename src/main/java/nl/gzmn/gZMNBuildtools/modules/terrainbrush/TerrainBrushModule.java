package nl.gzmn.gZMNBuildtools.modules.terrainbrush;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.command.TerrainBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.menu.TerrainBrushMenu;
import org.bukkit.entity.Player;

import java.util.List;

/** Terrain sculpting, tree and rock brushes (/terrainbrush). */
@SuppressWarnings("UnstableApiUsage")
public final class TerrainBrushModule implements GzmnModule {

    private TerrainBrushCommand terrainBrushCommand;

    @Override
    public String id() {
        return "terrain-brushes";
    }

    @Override
    public String description() {
        return "Terrain sculpting, tree and rock brushes (/terrainbrush).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        terrainBrushCommand = new TerrainBrushCommand(context.plugin(), context.core().messages(),
                context.core().playerData(), context.core().limits());
        context.commands(this::registerTerrainBrush);
    }

    private void registerTerrainBrush(Commands commands) {
        commands.register(Commands.literal("terrainbrush")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(TerrainBrushCommand.PERMISSION))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    new TerrainBrushMenu(player, terrainBrushCommand).open();
                    return 1;
                })
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestLoadouts)
                                .executes(ctx -> {
                                    terrainBrushCommand.saveLoadout((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestLoadouts)
                                .executes(ctx -> {
                                    terrainBrushCommand.loadLoadout((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestLoadouts)
                                .executes(ctx -> {
                                    terrainBrushCommand.deleteLoadout((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("loadouts").executes(ctx -> {
                    terrainBrushCommand.loadouts((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    terrainBrushCommand.list((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            terrainBrushCommand.typeSuggestions().stream()
                                    .filter(s -> s.startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            terrainBrushCommand.help((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "type"));
                            return 1;
                        })
                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> {
                                    terrainBrushCommand.bind((Player) ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "type"),
                                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius"),
                                            "");
                                    return 1;
                                })
                                .then(Commands.argument("args", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            terrainBrushCommand.argumentSuggestions(
                                                            StringArgumentType.getString(ctx, "type"),
                                                            builder.getRemaining())
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            terrainBrushCommand.bind((Player) ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "type"),
                                                    com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius"),
                                                    StringArgumentType.getString(ctx, "args"));
                                            return 1;
                                        }))))
                .build(), "Bind a GZMN terrain brush (raise, smooth, overlay, blob, ...) to your held item.",
                List.of("tbrush", "tb"));
    }

    private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestLoadouts(
            com.mojang.brigadier.context.CommandContext<io.papermc.paper.command.brigadier.CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        if (ctx.getSource().getSender() instanceof Player player) {
            terrainBrushCommand.loadoutNames(player).stream()
                    .filter(name -> name.startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}
