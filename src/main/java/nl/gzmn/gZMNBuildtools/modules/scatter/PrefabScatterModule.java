package nl.gzmn.gZMNBuildtools.modules.scatter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.block.BlockType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.scatter.command.ScatterCommand;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.ScatterPlanner;
import nl.gzmn.gZMNBuildtools.modules.scatter.prefab.PrefabLibrary;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** /gzscatter: a brush that scatters schematics, trees and rocks naturally over the ground. */
@SuppressWarnings("UnstableApiUsage")
public final class PrefabScatterModule implements GzmnModule {

    private volatile PrefabLibrary library;
    private ScatterCommand command;

    @Override
    public String id() {
        return "prefab-scatter";
    }

    @Override
    public String description() {
        return "Scatter schematics, trees and rocks over the ground with a brush (/gzscatter).";
    }

    @Override
    public void enable(ModuleContext context) {
        BlockKnowledge blocks = context.core().blocks();
        Set<String> canopy = Stream.concat(Tag.LEAVES.getValues().stream(), Tag.LOGS.getValues().stream())
                .map(PrefabScatterModule::id).collect(Collectors.toUnmodifiableSet());
        Map<String, int[]> materials = new ConcurrentHashMap<>();
        Predicate<String> solid = id -> material(blocks, materials, id)[0] == 1;
        Predicate<String> liquid = id -> material(blocks, materials, id)[1] == 1;
        ScatterPlanner.Rules rules = new ScatterPlanner.Rules(id -> solid.test(id) && !canopy.contains(id), solid,
                id -> blocks.rules().isTerrain(id) || id.equals("moss_block") || id.endsWith("terracotta"), liquid);
        load(context);
        command = new ScatterCommand(context.plugin(), context.core().messages(), () -> library, rules);
        context.commands(this::register);
    }

    @Override
    public void reload(ModuleContext context) {
        load(context);
    }

    private void load(ModuleContext context) {
        Path root = WorldEdit.getInstance().getWorkingDirectoryPath(
                WorldEdit.getInstance().getConfiguration().saveDir);
        int maxBlocks = Math.max(1, context.plugin().getConfig().getInt("scatter.max-prefab-blocks", 50_000));
        BlockKnowledge blocks = context.core().blocks();
        library = new PrefabLibrary(root, id -> blocks.rules().isTerrain(id), maxBlocks);
    }

    /** {solid, liquid} as 1/0, cached per block id. */
    private static int[] material(BlockKnowledge blocks, Map<String, int[]> cache, String id) {
        return cache.computeIfAbsent(id, k -> {
            BlockType type = blocks.type(k);
            if (type == null) {
                return new int[]{0, 0};
            }
            return new int[]{type.getMaterial().isSolid() ? 1 : 0, type.getMaterial().isLiquid() ? 1 : 0};
        });
    }

    private static String id(Material material) {
        return material.getKey().getKey().toLowerCase(Locale.ROOT);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzscatter")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(ScatterCommand.PERMISSION))
                .executes(ctx -> {
                    command.help((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("list").executes(ctx -> {
                    command.list((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.argument("prefabs", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                command.suggestions(player, builder.getRemaining()).forEach(builder::suggest);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.bind((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "prefabs"));
                            return 1;
                        }))
                .build(), "Scatter schematics, trees and rocks with a brush.", List.of("scatter"));
    }
}
