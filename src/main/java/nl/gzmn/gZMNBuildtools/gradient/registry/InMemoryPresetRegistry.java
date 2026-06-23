package nl.gzmn.gZMNBuildtools.gradient.registry;

import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Simple in-memory {@link PresetRegistry}. Used as a default (e.g. in tests) and
 * as the base for {@link YamlPresetRegistry}.
 */
public class InMemoryPresetRegistry implements PresetRegistry {

    protected final Map<String, GradientPreset> presets = new LinkedHashMap<>();

    @Override
    public Optional<GradientPreset> get(String id) {
        return Optional.ofNullable(presets.get(id));
    }

    @Override
    public List<GradientPreset> all() {
        return new ArrayList<>(presets.values());
    }

    @Override
    public List<String> ids() {
        return new ArrayList<>(presets.keySet());
    }

    @Override
    public List<GradientPreset> byCategory(GradientPreset.PresetCategory category) {
        return presets.values().stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public void register(GradientPreset preset) {
        presets.put(preset.getId(), preset);
    }

    @Override
    public int count() {
        return presets.size();
    }

    @Override
    public void reload() {
        // In-memory registry has no backing source.
    }
}
