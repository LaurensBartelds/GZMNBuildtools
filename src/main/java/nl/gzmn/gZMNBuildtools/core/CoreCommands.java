package nl.gzmn.gZMNBuildtools.core;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.ModuleManager;
import nl.gzmn.gZMNBuildtools.core.module.ModuleStatus;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Commands that belong to the plugin itself rather than a feature: admin and previews. */
@SuppressWarnings("UnstableApiUsage")
public final class CoreCommands {

    private final JavaPlugin plugin;
    private final PluginContext context;
    private final ModuleManager modules;

    public CoreCommands(JavaPlugin plugin, PluginContext context, ModuleManager modules) {
        this.plugin = plugin;
        this.context = context;
        this.modules = modules;
    }

    public void register(Commands commands) {
        commands.register(Commands.literal("gzmnbuildtools")
                .requires(source -> source.getSender().hasPermission("gzmnbuildtools.admin"))
                .executes(ctx -> {
                    listModules(ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("reload").executes(ctx -> {
                    reload(ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("modules").executes(ctx -> {
                    listModules(ctx.getSource().getSender());
                    return 1;
                }))
                .build(), "GZMNBuildtools admin commands", List.of("gzmnbt"));

        commands.register(Commands.literal("gzpreview")
                .requires(source -> source.getSender() instanceof Player)
                .then(Commands.literal("apply").executes(ctx -> {
                    context.previews().apply((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    context.previews().cancel((Player) ctx.getSource().getSender());
                    return 1;
                }))
                .build(), "Apply or cancel a GZMN preview.", List.of("gzp"));

        commands.register(Commands.literal("gzlimits")
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        showLimits(player, player);
                    } else {
                        ctx.getSource().getSender().sendMessage("Usage: /gzlimits <player>");
                    }
                    return 1;
                })
                .then(Commands.argument("player", ArgumentTypes.player())
                        .requires(source -> source.getSender().hasPermission("gzmnbuildtools.admin"))
                        .executes(ctx -> {
                            List<Player> found = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource());
                            for (Player target : found) {
                                showLimits(ctx.getSource().getSender(), target);
                            }
                            return 1;
                        }))
                .build(), "Show the brush and edit limits of your rank.", List.of("gzlimit"));
    }

    private void showLimits(CommandSender viewer, Player player) {
        Messages messages = context.messages();
        Player reader = viewer instanceof Player p ? p : null;
        Limits.Resolved limits = context.limits().of(player);
        String ranks = limits.ranks().isEmpty() ? messages.text(reader, "none") : String.join(", ", limits.ranks());
        viewer.sendMessage(Component.text(messages.text(reader, "Limits for %s (ranks: %s):", player.getName(), ranks),
                NamedTextColor.YELLOW));
        for (Limits.Kind kind : Limits.Kind.values()) {
            String source = limits.source(kind);
            String value = limits.unlimited() ? messages.text(reader, "unlimited")
                    : source == null ? messages.text(reader, "server setting")
                    : messages.text(reader, "%d (from %s)", limits.get(kind, 0), source);
            viewer.sendMessage(Component.text("  " + messages.text(reader, kind.label()) + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, source == null ? NamedTextColor.WHITE : NamedTextColor.AQUA)));
        }
    }

    private void reload(CommandSender sender) {
        plugin.reloadConfig();
        context.reload(plugin.getConfig());
        modules.reloadAll();
        sender.sendMessage(Component.text("GZMNBuildtools reloaded: " + context.presets().count()
                + " preset(s). Turning modules on or off needs a restart.", NamedTextColor.GREEN));
        plugin.getLogger().info("Configuration reloaded by " + sender.getName());
    }

    private void listModules(CommandSender sender) {
        sender.sendMessage(Component.text("GZMNBuildtools modules (switch them in config.yml, then restart):",
                NamedTextColor.YELLOW));
        for (ModuleStatus status : modules.statuses()) {
            NamedTextColor color = switch (status.state()) {
                case ENABLED -> NamedTextColor.GREEN;
                case DISABLED -> NamedTextColor.GRAY;
                case MISSING_DEPENDENCY, FAILED -> NamedTextColor.RED;
            };
            Component line = Component.text("  " + status.id() + " ", color)
                    .append(Component.text(status.state().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '),
                            color))
                    .append(Component.text(" - " + status.description(), NamedTextColor.GRAY));
            if (status.reason() != null) {
                line = line.append(Component.text(" (" + status.reason() + ")", NamedTextColor.DARK_GRAY));
            }
            sender.sendMessage(line);
        }
    }

    /** The core's own entries in the /gz help hub. */
    public static List<HelpEntry> help() {
        return List.of(
                HelpEntry.of(HelpEntry.Category.OTHER, Material.CLOCK, "/gzlimits", tr("Your limits"),
                        tr("See your brush radius, block and other limits, and which rank gives them.")).runs()
                        .build("core"),
                HelpEntry.of(HelpEntry.Category.OTHER, Material.COMPARATOR, "/gzmnbuildtools", tr("Modules"),
                        tr("See which features are running; /gzmnbuildtools reload re-reads the configuration."))
                        .runs().permission("gzmnbuildtools.admin").build("core"));
    }

    /** One-line summary for the startup log. */
    public static String summary(ModuleManager modules) {
        StringBuilder sb = new StringBuilder();
        for (ModuleStatus status : modules.statuses()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(status.id()).append(switch (status.state()) {
                case ENABLED -> "";
                case DISABLED -> " (off)";
                case MISSING_DEPENDENCY -> " (missing dependency)";
                case FAILED -> " (FAILED)";
            });
        }
        return sb.toString();
    }
}
