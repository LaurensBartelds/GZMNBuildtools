package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Each player's current gradient settings. The bound brush, the GUI, the
 * web editor and commands all read and edit the same entry, so a change made
 * anywhere applies to the brush immediately.
 */
public final class GradientSettingsStore {

    /** Where settings are kept between restarts. */
    public interface Persistence {
        Optional<GradientSettings> load(UUID player);

        /** @param settings null when the player reset to defaults */
        void save(UUID player, GradientSettings settings);

        Persistence NONE = new Persistence() {
            @Override
            public Optional<GradientSettings> load(UUID player) {
                return Optional.empty();
            }

            @Override
            public void save(UUID player, GradientSettings settings) {
            }
        };
    }

    private final Map<UUID, GradientSettings> settings = new ConcurrentHashMap<>();
    private final Persistence persistence;

    public GradientSettingsStore() {
        this(Persistence.NONE);
    }

    public GradientSettingsStore(Persistence persistence) {
        this.persistence = persistence;
    }

    public GradientSettings get(UUID player) {
        return settings.computeIfAbsent(player,
                id -> persistence.load(id).orElseGet(GradientSettings::defaults));
    }

    public GradientSettings update(UUID player, UnaryOperator<GradientSettings> change) {
        GradientSettings updated = change.apply(get(player));
        set(player, updated);
        return updated;
    }

    public void set(UUID player, GradientSettings value) {
        settings.put(player, value);
        persistence.save(player, value);
    }

    public void reset(UUID player) {
        settings.put(player, GradientSettings.defaults());
        persistence.save(player, null);
    }
}
