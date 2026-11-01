package nl.gzmn.gZMNBuildtools.gradient.smart;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Each player's current gradient settings. The bound brush, the GUI, the
 * web editor and commands all read and edit the same entry, so a change made
 * anywhere applies to the brush immediately.
 */
public final class GradientSettingsStore {

    private final Map<UUID, GradientSettings> settings = new ConcurrentHashMap<>();

    public GradientSettings get(UUID player) {
        return settings.getOrDefault(player, GradientSettings.defaults());
    }

    public GradientSettings update(UUID player, UnaryOperator<GradientSettings> change) {
        return settings.compute(player, (id, old) -> change.apply(old == null ? GradientSettings.defaults() : old));
    }

    public void set(UUID player, GradientSettings value) {
        settings.put(player, value);
    }

    public void reset(UUID player) {
        settings.remove(player);
    }
}
