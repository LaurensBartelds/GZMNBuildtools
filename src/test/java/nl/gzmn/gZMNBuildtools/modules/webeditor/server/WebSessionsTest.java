package nl.gzmn.gZMNBuildtools.modules.webeditor.server;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class WebSessionsTest {

    @Test
    void tokensResolveToTheirPlayerUntilTheyExpire() {
        AtomicLong now = new AtomicLong(1_000);
        WebSessions sessions = new WebSessions(60_000, now::get);
        UUID player = UUID.randomUUID();
        String token = sessions.create(player);
        assertTrue(token.length() >= 30, "token should be long and unguessable");
        assertEquals(player, sessions.resolve(token).orElseThrow());
        now.addAndGet(61_000);
        assertTrue(sessions.resolve(token).isEmpty());
    }

    @Test
    void aNewLinkReplacesThePlayersOldOne() {
        WebSessions sessions = new WebSessions(60_000);
        UUID player = UUID.randomUUID();
        String first = sessions.create(player);
        String second = sessions.create(player);
        assertNotEquals(first, second);
        assertTrue(sessions.resolve(first).isEmpty());
        assertEquals(player, sessions.resolve(second).orElseThrow());
        assertTrue(sessions.resolve(null).isEmpty());
        assertTrue(sessions.resolve("nope").isEmpty());
    }

    @Test
    void queryStringsAreDecoded() {
        Map<String, String> q = WebApi.query("spec=%5B50%25stone%5D&steps=5&flag");
        assertEquals("[50%stone]", q.get("spec"));
        assertEquals("5", q.get("steps"));
        assertEquals("", q.get("flag"));
    }
}
