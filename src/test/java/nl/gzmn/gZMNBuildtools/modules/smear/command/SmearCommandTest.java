package nl.gzmn.gZMNBuildtools.modules.smear.command;

import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SmearCommandTest {

    private static final class FakeMessages implements Messages {
        final List<String> errors = new ArrayList<>();

        @Override
        public Component prefixed(Component body) {
            return body;
        }

        @Override
        public void send(Player player, Component body) {
        }

        @Override
        public void info(Player player, String template, Object... args) {
        }

        @Override
        public void success(Player player, String template, Object... args) {
        }

        @Override
        public void warn(Player player, String template, Object... args) {
        }

        @Override
        public void error(Player player, String template, Object... args) {
            errors.add(String.format(Locale.ROOT, template, args));
        }

        @Override
        public void verbose(Player player, String template, Object... args) {
        }

        @Override
        public boolean isVerbose() {
            return false;
        }

        @Override
        public void setVerbose(boolean verbose) {
        }
    }

    private FakeMessages messages;
    private SmearCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new SmearCommand(mock(JavaPlugin.class), messages, mock(Limits.class));
    }

    @Test
    void unknownOptionShowsError() {
        command.bind(mock(Player.class), "foo=1");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Unknown option 'foo'"));
    }

    @Test
    void badAmountShowsError() {
        command.bind(mock(Player.class), "amount=99");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("amount must be"));
    }

    @Test
    void badDirectionShowsError() {
        command.bind(mock(Player.class), "direction=sideways");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("direction must be"));
    }

    @Test
    void suggestsOptions() {
        assertTrue(command.suggestions("amo").contains("amount="));
        assertTrue(command.suggestions("direction=").contains("direction=stroke"));
    }
}
