package nl.gzmn.gZMNBuildtools.modules.shadow.command;

import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
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

class ShadowCommandTest {

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
    private ShadowCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new ShadowCommand(mock(JavaPlugin.class), messages, mock(BuildEditor.class),
                mock(PreviewService.class), mock(Limits.class));
    }

    @Test
    void emptyArgsShowsErrorUsage() {
        command.shadow(mock(Player.class), "", false);
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("/gzshadow"));
    }

    @Test
    void distOutOfRangeShowsParseError() {
        command.shadow(mock(Player.class), "stone dist=99", false);
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("dist must be between"));
    }

    @Test
    void badDirectionShowsParseError() {
        command.shadow(mock(Player.class), "stone dir=up", false);
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("dir must be one of"));
    }

    @Test
    void usageListsParams() {
        command.usage(mock(Player.class));
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("/gzshadow")));
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("dir")));
        assertTrue(messages.infos.stream().anyMatch(s -> s.contains("dist")));
    }
}
