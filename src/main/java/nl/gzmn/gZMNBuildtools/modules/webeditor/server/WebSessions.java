package nl.gzmn.gZMNBuildtools.modules.webeditor.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Personal web editor links: an unguessable token per player that lets the
 * browser read and change that player's gradient brush until it expires.
 */
public final class WebSessions {

    private record Session(UUID player, long expiresAt) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long lifetimeMillis;

    public WebSessions(long lifetimeMillis) {
        this(lifetimeMillis, System::currentTimeMillis);
    }

    WebSessions(long lifetimeMillis, LongSupplier clock) {
        this.lifetimeMillis = lifetimeMillis;
        this.clock = clock;
    }

    /** Creates a new token for the player, replacing their previous one. */
    public String create(UUID player) {
        sessions.values().removeIf(s -> s.player.equals(player) || s.expiresAt < clock.getAsLong());
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(player, clock.getAsLong() + lifetimeMillis));
        return token;
    }

    public Optional<UUID> resolve(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Session s = sessions.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (s.expiresAt < clock.getAsLong()) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.player);
    }

    public long lifetimeMinutes() {
        return lifetimeMillis / 60_000L;
    }
}
