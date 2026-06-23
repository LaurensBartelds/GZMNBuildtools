package nl.gzmn.gZMNBuildtools.ui.typereplace;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory per-player state for the Type Replace GUI.
 *
 * <p>Replaces the previous approach of stashing UI flags in each player's
 * {@link org.bukkit.persistence.PersistentDataContainer}, which polluted player
 * data and relied on an unreliable delayed cleanup. State here is cleared as
 * soon as the player closes the menu or quits.</p>
 */
public class TypeReplaceSessions {

    /** Mutable selection state for one player's GUI session. */
    public static final class Session {
        String sourceMaterial;
        String targetMaterial;
        String selecting;        // "source" or "target"
        Integer categoryOrdinal; // index into MaterialCategory
    }

    private final Map<UUID, Session> sessions = new HashMap<>();

    public Session getOrCreate(UUID playerId) {
        return sessions.computeIfAbsent(playerId, k -> new Session());
    }

    public Session get(UUID playerId) {
        return sessions.get(playerId);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }
}
