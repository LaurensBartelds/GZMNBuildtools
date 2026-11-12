package nl.gzmn.gZMNBuildtools.modules.revolve.command;

import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
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

class RevolveCommandTest {

    private static final class FakeMessages implements Messages {
        final List<String> errors = new ArrayList<>();
        final List<String> infos = new ArrayList<>();

        @Override
        public Component prefixed(Component body) {
            return body;
        }

        @Override
        public void send(Player player, Component body) {
        }

        @Override
        public void info(Player player, String template, Object... args) {
            infos.add(String.format(Locale.ROOT, template, args));
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
    private RevolveCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new RevolveCommand(mock(JavaPlugin.class), messages, mock(PreviewService.class), mock(Limits.class));
    }

    @Test
    void unknownParameterShowsError() {
        command.revolve(mock(Player.class), "stone foo=1");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Unknown parameter 'foo'"));
    }

    @Test
    void emptyArgsShowUsage() {
        command.revolve(mock(Player.class), "");
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("/gzrevolve")));
    }

    @Test
    void suggestsAxisChoices() {
        assertTrue(command.suggestions("axis=").contains("axis=y"));
        assertTrue(command.suggestions("st").stream().anyMatch(s -> s.startsWith("stone_bricks")));
    }
}
