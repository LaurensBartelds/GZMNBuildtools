package nl.gzmn.gZMNBuildtools.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdventureMessagesTest {

    @Test
    void prefixedComponent_hasExpectedPlainText_andColors() {
        AdventureMessages messages = AdventureMessages.basic();
        Component body = Component.text("Replaced 5 blocks.");
        Component prefixed = messages.prefixed(body);

        String plain = PlainTextComponentSerializer.plainText().serialize(prefixed);
        assertEquals("[GZMN] Replaced 5 blocks.", plain);

        boolean found = false;
        for (var child : prefixed.children()) {
            String txt = PlainTextComponentSerializer.plainText().serialize(child);
            if ("GZMN".equals(txt)) {
                var color = child.style().color();
                assertNotNull(color, "label color should be present");
                assertEquals(NamedTextColor.GREEN, color);
                found = true;
                break;
            }
            for (var sub : child.children()) {
                if ("GZMN".equals(PlainTextComponentSerializer.plainText().serialize(sub))) {
                    var color = sub.style().color();
                    assertNotNull(color, "label color should be present");
                    assertEquals(NamedTextColor.GREEN, color);
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
        assertTrue(found, "expected to find a child with text 'GZMN'");
    }

    @Test
    void verbose_respectsFlag() {
        AdventureMessages quiet = new AdventureMessages(false);
        assertFalse(quiet.isVerbose());
        quiet.setVerbose(true);
        assertTrue(quiet.isVerbose());
    }
}
