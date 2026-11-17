package nl.gzmn.gZMNBuildtools.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.entity.Player;

import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Adventure-backed implementation of {@link Messages}.
 */
public final class AdventureMessages implements Messages {

    private volatile boolean verbose;
    private volatile String prefix = "GZMN";
    private volatile Translations translations = Translations.NONE;

    public AdventureMessages(boolean verbose) {
        this.verbose = verbose;
    }

    /** The tag shown in front of every message (messages.prefix in config.yml). */
    public void setPrefix(String prefix) {
        this.prefix = prefix == null || prefix.isBlank() ? "GZMN" : prefix;
    }

    /** A messages instance with verbose output disabled (used as a default). */
    public static AdventureMessages basic() {
        return new AdventureMessages(false);
    }

    @Override
    public Component prefixed(Component body) {
        Component label = Component.text("[").color(NamedTextColor.WHITE)
                .append(Component.text(prefix).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("] ").color(NamedTextColor.WHITE));
        return label.append(body.color(NamedTextColor.GRAY));
    }

    @Override
    public void send(Player player, Component body) {
        player.sendMessage(prefixed(body));
    }

    public void setTranslations(Translations translations) {
        this.translations = translations;
    }

    public Translations translations() {
        return translations;
    }

    @Override
    public String text(Player player, String template, Object... args) {
        String translated = translations.translate(template, player == null ? null : player.locale());
        if (!translated.equals(template)) {
            try {
                return format(translated, args);
            } catch (IllegalFormatException e) {
                // A translation with the wrong placeholders: fall back to English.
            }
        }
        return format(template, args);
    }

    @Override
    public void info(Player player, String template, Object... args) {
        send(player, Component.text(text(player, template, args)).color(NamedTextColor.YELLOW));
    }

    @Override
    public void success(Player player, String template, Object... args) {
        send(player, Component.text(text(player, template, args)).color(NamedTextColor.GRAY));
    }

    @Override
    public void warn(Player player, String template, Object... args) {
        info(player, template, args);
    }

    @Override
    public void error(Player player, String template, Object... args) {
        send(player, Component.text(text(player, template, args)).color(NamedTextColor.RED));
    }

    @Override
    public void verbose(Player player, String template, Object... args) {
        if (verbose) {
            send(player, Component.text(text(player, template, args)).color(NamedTextColor.GRAY));
        }
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private static String format(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }

    public static String asPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String asLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
