package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.part.CommandArgument;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.part.CommandParts;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Registers WorldEdit-style {@code //} aliases (e.g. {@code //roof}) in FAWE's
 * command manager; they forward to the plugin's {@code /gz*} command handlers.
 */
public final class WeAliases {

    private WeAliases() {
    }

    /**
     * @param name      the WorldEdit command name, without slashes ({@code roof} makes {@code //roof} work)
     * @param permission the Bukkit permission the sender needs
     * @param handler   receives the player and the joined arguments
     */
    public static void register(JavaPlugin plugin, String name, String permission, BiConsumer<Player, String> handler) {
        try {
            CommandManager manager = WorldEdit.getInstance().getPlatformManager().getPlatformCommandManager()
                    .getCommandManager();
            if (manager.containsCommand(name)) {
                return;
            }
            CommandArgument args = CommandParts.arg(TranslatableComponent.of("args"),
                            TextComponent.of("arguments"))
                    .variable(true)
                    .defaultsTo(List.of())
                    .build();
            manager.register(name, builder -> builder
                    .description(TextComponent.of("GZMNBuildtools alias for /gz" + name))
                    .condition(params -> {
                        Actor actor = params.injectedValue(Key.of(Actor.class)).orElse(null);
                        return actor != null && actor.hasPermission(permission);
                    })
                    .addPart(args)
                    .action(params -> {
                        Actor actor = params.injectedValue(Key.of(Actor.class)).orElse(null);
                        if (!(actor instanceof com.sk89q.worldedit.entity.Player wePlayer)) {
                            return 0;
                        }
                        Player player = BukkitAdapter.adapt(wePlayer);
                        handler.accept(player, String.join(" ", params.valueOf(args).asStrings()));
                        return 1;
                    }));
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not register the //" + name + " alias (WorldEdit/FAWE command API unavailable?)", t);
        }
    }
}
