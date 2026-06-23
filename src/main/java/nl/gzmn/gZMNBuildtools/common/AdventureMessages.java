package nl.gzmn.gZMNBuildtools.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Adventure-backed implementation of {@link Messages}.
 */
public final class AdventureMessages implements Messages {

    private volatile boolean verbose;

    public AdventureMessages(boolean verbose) {
        this.verbose = verbose;
    }

    /** A messages instance with verbose output disabled (used as a default). */
    public static AdventureMessages basic() {
        return new AdventureMessages(false);
    }

    @Override
    public Component prefixed(Component body) {
        Component label = Component.text("[").color(NamedTextColor.WHITE)
                .append(Component.text("GZMN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("] ").color(NamedTextColor.WHITE));
        return label.append(body.color(NamedTextColor.GRAY));
    }

    @Override
    public void send(Player player, Component body) {
        player.sendMessage(prefixed(body));
    }

    @Override
    public void info(Player player, String template, Object... args) {
        send(player, Component.text(format(template, args)).color(NamedTextColor.YELLOW));
    }

    @Override
    public void success(Player player, String template, Object... args) {
        send(player, Component.text(format(template, args)).color(NamedTextColor.GRAY));
    }

    @Override
    public void warn(Player player, String template, Object... args) {
        info(player, template, args);
    }

    @Override
    public void error(Player player, String template, Object... args) {
        send(player, Component.text(format(template, args)).color(NamedTextColor.RED));
    }

    @Override
    public void verbose(Player player, String template, Object... args) {
        if (verbose) {
            send(player, Component.text(format(template, args)).color(NamedTextColor.GRAY));
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
