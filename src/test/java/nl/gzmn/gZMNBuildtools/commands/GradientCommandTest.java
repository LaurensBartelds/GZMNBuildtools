package nl.gzmn.gZMNBuildtools.commands;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradientCommandTest {

    @Test
    void openUI_withoutUiManager_sendsPrefixedError() {
        Player player = mock(Player.class);
        GradientCommand cmd = new GradientCommand(null);

        cmd.openUI(player);

        ArgumentCaptor<Component> cap = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(cap.capture());

        String plain = PlainTextComponentSerializer.plainText().serialize(cap.getValue());
        assertTrue(plain.startsWith("[GZMN]"), "message should be prefixed");
        assertTrue(plain.contains("Gradient UI is not available."));
    }

    @Test
    void execute_invalidDirection_sendsErrorAndHelp() {
        Player player = mock(Player.class);
        GradientCommand cmd = new GradientCommand(null);

        cmd.execute(player, "stone,cobble", "BAD_DIR", "LINEAR");

        ArgumentCaptor<Component> cap = ArgumentCaptor.forClass(Component.class);
        // expect at least 3 messages: error + directions + modes
        verify(player, atLeast(3)).sendMessage(cap.capture());

        var all = cap.getAllValues();
        String first = PlainTextComponentSerializer.plainText().serialize(all.get(0));
        assertTrue(first.startsWith("[GZMN]"));
        assertTrue(first.contains("Error:"));

        // find the help lines
        boolean foundDir = all.stream()
                .anyMatch(c -> PlainTextComponentSerializer.plainText().serialize(c).contains("Valid directions"));
        boolean foundMode = all.stream()
                .anyMatch(c -> PlainTextComponentSerializer.plainText().serialize(c).contains("Valid modes"));
        assertTrue(foundDir && foundMode);
    }

    @Test
    void execute_noSelection_sendsSelectionError() throws Exception {
        Player player = mock(Player.class);
        GradientCommand cmd = new GradientCommand(null) {
            @Override
            protected com.sk89q.worldedit.regions.Region getSelectionFromPlayer(org.bukkit.entity.Player p) {
                return null;
            }
        };

        cmd.execute(player, "stone,cobble", "VERTICAL_UP", "LINEAR");

        ArgumentCaptor<Component> cap = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(cap.capture());
        String plain = PlainTextComponentSerializer.plainText().serialize(cap.getValue());
        assertTrue(plain.startsWith("[GZMN]"));
        assertTrue(plain.contains("Selection required"));
    }
}
