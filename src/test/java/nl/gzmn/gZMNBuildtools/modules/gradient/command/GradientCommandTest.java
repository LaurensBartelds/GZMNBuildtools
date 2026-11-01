package nl.gzmn.gZMNBuildtools.modules.gradient.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradientCommandTest {

    @Test
    void execute_invalidDirection_sendsErrorAndHelp() {
        Player player = mock(Player.class);
        GradientCommand cmd = new GradientCommand();

        cmd.execute(player, "stone,cobble", "BAD_DIR", "LINEAR");

        ArgumentCaptor<Component> cap = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeast(3)).sendMessage(cap.capture());

        var all = cap.getAllValues();
        String first = PlainTextComponentSerializer.plainText().serialize(all.get(0));
        assertTrue(first.startsWith("[GZMN]"));
        assertTrue(first.contains("Error:"));

        boolean foundDir = all.stream()
                .anyMatch(c -> PlainTextComponentSerializer.plainText().serialize(c).contains("Valid directions"));
        boolean foundMode = all.stream()
                .anyMatch(c -> PlainTextComponentSerializer.plainText().serialize(c).contains("Valid modes"));
        assertTrue(foundDir && foundMode);
    }

    @Test
    void execute_noSelection_sendsSelectionError() throws Exception {
        Player player = mock(Player.class);
        GradientCommand cmd = new GradientCommand() {
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
