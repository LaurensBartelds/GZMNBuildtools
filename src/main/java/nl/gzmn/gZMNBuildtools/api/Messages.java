package nl.gzmn.gZMNBuildtools.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Sends prefixed, colour-coded messages to players.
 *
 * <p>Previously this was a static utility ({@code MessageManager}); it is now an
 * injected service so commands can be tested in isolation and so the verbosity
 * setting can be reloaded at runtime.</p>
 */
public interface Messages {

    /** Wrap a message body with the {@code [GZMN]} prefix. */
    Component prefixed(Component body);

    /** Send a pre-built component, prefixed. */
    void send(Player player, Component body);

    /** Informational message (yellow). Template is {@link String#format} style. */
    void info(Player player, String template, Object... args);

    /** Success message (gray). */
    void success(Player player, String template, Object... args);

    /** Warning message. */
    void warn(Player player, String template, Object... args);

    /** Error message (red). */
    void error(Player player, String template, Object... args);

    /** Diagnostic message, only sent when {@link #isVerbose()} is true. */
    void verbose(Player player, String template, Object... args);

    boolean isVerbose();

    void setVerbose(boolean verbose);
}
