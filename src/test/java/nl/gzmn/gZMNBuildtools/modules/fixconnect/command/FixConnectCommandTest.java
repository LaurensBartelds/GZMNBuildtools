package nl.gzmn.gZMNBuildtools.modules.fixconnect.command;

import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
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

class FixConnectCommandTest {

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
    private FixConnectCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new FixConnectCommand(mock(JavaPlugin.class), messages, mock(PreviewService.class),
                mock(Limits.class), mock(BlockKnowledge.class));
    }

    @Test
    void unexpectedArgumentsShowUsage() {
        command.fix(mock(Player.class), "bogus");
        assertEquals(0, messages.errors.size());
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("/gzfix")));
    }

    @Test
    void usageMentionsPreviewFlag() {
        command.usage(mock(Player.class));
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("-p")));
    }
}
