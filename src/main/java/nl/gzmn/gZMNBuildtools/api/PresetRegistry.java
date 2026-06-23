package nl.gzmn.gZMNBuildtools.api;

import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;

import java.util.List;
import java.util.Optional;

/**
 * Registry of gradient presets. Backed by {@code presets.yml} at runtime, but
 * other plugins can contribute presets via {@link #register(GradientPreset)}.
 */
public interface PresetRegistry {

    Optional<GradientPreset> get(String id);

    List<GradientPreset> all();

    List<String> ids();

    List<GradientPreset> byCategory(GradientPreset.PresetCategory category);

    void register(GradientPreset preset);

    int count();

    /** Reload presets from their backing source (no-op for in-memory registries). */
    void reload();
}
