package nl.gzmn.gZMNBuildtools.modules.pixelart.command;

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

class TextCommandTest {

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
    private TextCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new TextCommand(mock(JavaPlugin.class), messages, mock(BlockKnowledge.class),
                mock(PreviewService.class), mock(Limits.class));
    }

    @Test
    void tooFewArgsShowsUsage() {
        command.text(mock(Player.class), "stone 3");
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("/gztext")));
    }

    @Test
    void badSizeShowsError() {
        command.text(mock(Player.class), "stone abc hello");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Size must be"));
    }

    @Test
    void unknownFontShowsError() {
        command.font(mock(Player.class), "wingdings");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Unknown font 'wingdings'"));
    }
}
