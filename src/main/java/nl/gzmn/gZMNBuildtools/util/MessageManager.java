package nl.gzmn.gZMNBuildtools.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Centralized message helper for building prefixed, colored Components and
 * sending concise/verbose messages. Designed to be minimal and safe to call
 * from commands/UI.
 */
public final class MessageManager {

    private static volatile boolean initialized = false;
    private static JavaPlugin plugin;
    private static boolean verbose = false;

    private MessageManager() {
    }

    public static void init(JavaPlugin plugin) {
        if (initialized)
            return;
        MessageManager.plugin = plugin;
        // read config flag if present; default to false
        try {
            verbose = plugin.getConfig().getBoolean("messages.verbose", false);
        } catch (Exception ignored) {
            verbose = false;
        }
        initialized = true;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static Component prefixed(Component body) {
        // Build: '[' (white) + 'GZMN' (lime) + ']' (white) + ' ' + body (light gray)
        Component label = Component.text("[").color(NamedTextColor.WHITE)
                .append(Component.text("GZMN").color(NamedTextColor.GREEN))
                .append(Component.text("] ").color(NamedTextColor.WHITE));
        // Ensure body uses the standard body color unless it already has a color
        Component bodyColored = body.color(NamedTextColor.GRAY);
        return label.append(bodyColored);
    }

    /* Generic send helpers */
    public static void send(Player player, Component body) {
        player.sendMessage(prefixed(body));
    }

    public static void info(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.YELLOW));
    }

    public static void success(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        // main body is gray; highlight numbers if caller supplies them separately when
        // needed
        send(player, Component.text(text).color(NamedTextColor.GRAY));
    }

    public static void warn(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.YELLOW));
    }

    public static void error(Player player, String template, Object... args) {
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.RED));
    }

    /**
     * Developer / verbose lines — only sent when verbose mode is enabled.
     */
    public static void verbose(Player player, String template, Object... args) {
        if (!isVerbose())
            return;
        String text = String.format(Locale.ROOT, template, args);
        send(player, Component.text(text).color(NamedTextColor.GRAY));
    }

    /* Helpers for legacy APIs (inventory display names, ItemMeta) */
    public static String asLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String asPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
