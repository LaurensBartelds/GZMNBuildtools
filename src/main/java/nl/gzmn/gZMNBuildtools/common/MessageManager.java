package nl.gzmn.gZMNBuildtools.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class MessageManager {

    private static volatile boolean initialized = false;
    private static JavaPlugin plugin;
    private static boolean verbose = false;

    private MessageManager() {
    }

    public static void init(JavaPlugin plugin) {
        if (initialized)
            return;
        apply(plugin);
        initialized = true;
    }

    /**
     * Re-read settings from the (already reloaded) plugin config. Used by the
     * /gzmnbuildtools reload command so changes take effect without a restart.
     */
    public static void reload(JavaPlugin plugin) {
        apply(plugin);
    }

    private static void apply(JavaPlugin plugin) {
        MessageManager.plugin = plugin;
        try {
            verbose = plugin.getConfig().getBoolean("messages.verbose", false);
        } catch (Exception ignored) {
            verbose = false;
        }
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static Component prefixed(Component body) {
        Component label = Component.text("[").color(NamedTextColor.WHITE)
                .append(Component.text("GZMN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("] ").color(NamedTextColor.WHITE));
        Component bodyColored = body.color(NamedTextColor.GRAY);
        return label.append(bodyColored);
    }

    public static void send(Player player, Component body) {
        player.sendMessage(prefixed(body));
    }

    public static void info(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.YELLOW));
    }

    public static void success(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.GRAY));
    }

    public static void warn(Player player, String template, Object... args) {
        info(player, template, args);
    }

    public static void error(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.RED));
    }

    public static void verbose(Player player, String template, Object... args) {
        if (!isVerbose())
            return;
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.GRAY));
    }

    public static String asLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String asPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
