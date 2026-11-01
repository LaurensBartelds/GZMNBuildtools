package nl.gzmn.gZMNBuildtools.modules.terraintexture;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.command.TextureCommand;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.TexturePresets;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.Texturer;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.fawe.TextureRunner;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** /gztexture: one-click terrain looks (alpine, desert, ...) for a selection or as a brush. */
@SuppressWarnings("UnstableApiUsage")
public final class TerrainTextureModule implements GzmnModule {

    static final String FILE = "terrain-presets.yml";

    private TextureRunner runner;
    private TextureCommand command;

    @Override
    public String id() {
        return "terrain-texture";
    }

    @Override
    public String description() {
        return "Texture terrain by slope, height and depth with presets (/gztexture).";
    }

    @Override
    public void enable(ModuleContext context) {
        Set<String> canopy = ids(Stream.concat(Tag.LEAVES.getValues().stream(), Tag.LOGS.getValues().stream()));
        Set<String> soil = ids(Tag.DIRT.getValues().stream());
        soil.add("farmland");
        Set<String> plants = ids(Stream.concat(Tag.FLOWERS.getValues().stream(), Tag.SAPLINGS.getValues().stream()));
        plants.addAll(List.of("short_grass", "tall_grass", "fern", "large_fern", "bush", "sweet_berry_bush"));
        runner = new TextureRunner(context.core().blocks(), Set.copyOf(canopy),
                new Texturer.Plants(Set.copyOf(plants)::contains, Set.copyOf(soil)::contains));
        load(context);
        command = new TextureCommand(context.plugin(), context.core().messages(), runner, context.core().previews());
        context.commands(this::register);
    }

    @Override
    public void reload(ModuleContext context) {
        load(context);
    }

    private void load(ModuleContext context) {
        File file = new File(context.plugin().getDataFolder(), FILE);
        if (!file.exists()) {
            context.plugin().saveResource(FILE, false);
        }
        BlockKnowledge blocks = context.core().blocks();
        TexturePresets presets = TexturePresets.parse(YamlConfiguration.loadConfiguration(file),
                id -> blocks.type(id) != null);
        presets.problems().forEach(p -> context.logger().warning(FILE + ": " + p));
        runner.presets(presets);
        context.logger().info("Loaded " + presets.ids().size() + " terrain presets.");
    }

    private static Set<String> ids(Stream<Material> materials) {
        Set<String> out = new HashSet<>();
        materials.forEach(m -> out.add(m.getKey().getKey().toLowerCase(Locale.ROOT)));
        return out;
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gztexture")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(TextureCommand.PERMISSION))
                .executes(ctx -> {
                    command.list((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.run((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"));
                            return 1;
                        }))
                .build(), "Texture terrain with a preset (alpine, desert, forest, ...).", List.of("texture"));
    }
}
