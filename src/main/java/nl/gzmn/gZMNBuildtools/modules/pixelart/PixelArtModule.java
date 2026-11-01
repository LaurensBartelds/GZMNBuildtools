package nl.gzmn.gZMNBuildtools.modules.pixelart;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.pixelart.command.PixelArtCommand;
import nl.gzmn.gZMNBuildtools.modules.pixelart.image.ImageLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** /gzpixelart: pictures and map art built from blocks. */
@SuppressWarnings("UnstableApiUsage")
public final class PixelArtModule implements GzmnModule {

    private PixelArtCommand command;

    @Override
    public String id() {
        return "pixel-art";
    }

    @Override
    public String description() {
        return "Build pictures and map art from images (/gzpixelart).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        Path folder = context.plugin().getDataFolder().toPath().resolve("images");
        Files.createDirectories(folder);
        FileConfiguration config = context.plugin().getConfig();
        ImageLoader loader = new ImageLoader(folder, new ImageLoader.Limits(
                Math.max(1, config.getInt("pixel-art.max-download-kb", 8192)) * 1024L,
                Math.max(64, config.getInt("pixel-art.max-image-dimension", 4096)),
                config.getBoolean("pixel-art.allow-urls", true),
                config.getBoolean("pixel-art.allow-private-addresses", false)));
        command = new PixelArtCommand(context.plugin(), context.core().messages(), context.core().blocks(),
                context.core().previews(), loader);
        context.commands(this::register);
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzpixelart")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(PixelArtCommand.PERMISSION))
                .executes(ctx -> {
                    command.help((Player) ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            command.suggestions(builder.getRemaining()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            command.build((Player) ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "options"));
                            return 1;
                        }))
                .build(), "Build a picture from blocks.", List.of("pixelart"));
    }
}
