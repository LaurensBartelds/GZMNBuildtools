package nl.gzmn.gZMNBuildtools.modules.typereplace.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypeReplaceCommandTest {

    @Test
    void getSuggestions_containsKnownMaterialGroup() {
        TypeReplaceCommand cmd = new TypeReplaceCommand();
        var suggestions = cmd.getSuggestions();
        assertTrue(suggestions.size() > 0);
        assertTrue(suggestions.stream()
                .anyMatch(s -> s.toLowerCase().contains("copper") || s.toLowerCase().contains("stone")));
    }

    @Test
    void execute_noSelection_sendsSelectionError() throws Exception {
        Player player = mock(Player.class);
        TypeReplaceCommand cmd = new TypeReplaceCommand() {
            @Override
            protected com.sk89q.worldedit.regions.Region getSelectionFromPlayer(org.bukkit.entity.Player p) {
                return null;
            }
        };

        cmd.execute(player, "stone", "copper");

        ArgumentCaptor<Component> cap = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(cap.capture());
        String plain = PlainTextComponentSerializer.plainText().serialize(cap.getValue());
        assertTrue(plain.startsWith("[GZMN]"));
        assertTrue(plain.contains("Selection required"));
    }
}
