package nl.gzmn.gZMNBuildtools.modules.loft.command;

import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoftCommandTest {

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
    private LoftCommand command;

    @BeforeEach
    void setUp() {
        messages = new FakeMessages();
        command = new LoftCommand(mock(JavaPlugin.class), messages, mock(PreviewService.class), mock(Limits.class));
    }

    private static Player fakePlayer() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        return player;
    }

    @Test
    void buildWithoutPointsShowsError() {
        command.build(fakePlayer(), "oak_planks");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Add at least two loft points first"));
    }

    @Test
    void unknownParameterShowsError() {
        command.build(fakePlayer(), "oak_planks foo=1");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("Unknown parameter 'foo'"));
    }

    @Test
    void emptyBuildArgsShowUsage() {
        command.build(fakePlayer(), "");
        assertEquals(1, messages.errors.size());
        assertTrue(messages.errors.get(0).contains("/gzloft build"));
    }

    @Test
    void suggestsParams() {
        assertTrue(command.suggestions("oak_planks outline=").contains("oak_planks outline="));
        assertTrue(command.suggestions("oa").stream().anyMatch(s -> s.startsWith("oak_planks")));
    }
}
