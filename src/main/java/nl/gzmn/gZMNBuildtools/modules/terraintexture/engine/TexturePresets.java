package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** The presets read from {@code terrain-presets.yml}, plus anything wrong with the file. */
public record TexturePresets(Map<String, TexturePreset> byId, List<String> problems) {

    public static final TexturePresets EMPTY = new TexturePresets(Map.of(), List.of());

    /** Reads the {@code presets:} section; broken presets are skipped and reported in {@link #problems()}. */
    public static TexturePresets parse(ConfigurationSection root, Predicate<String> isBlock) {
        ConfigurationSection section = root.getConfigurationSection("presets");
        if (section == null) {
            return new TexturePresets(Map.of(), List.of("the file has no 'presets:' section"));
        }
        Map<String, TexturePreset> presets = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            ConfigurationSection preset = section.getConfigurationSection(key);
            if (preset == null) {
                problems.add("preset '" + id + "' must be a section with description and rules");
                continue;
            }
            try {
                presets.put(id, TexturePreset.fromMap(id, preset.getValues(false), isBlock));
            } catch (IllegalArgumentException e) {
                problems.add(e.getMessage());
            }
        }
        return new TexturePresets(Collections.unmodifiableMap(presets), List.copyOf(problems));
    }

    public Optional<TexturePreset> get(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Set<String> ids() {
        return byId.keySet();
    }

    /** Every block any preset places, so re-texturing with another preset can repaint them. */
    public Set<String> allBlocks() {
        Set<String> out = new LinkedHashSet<>();
        byId.values().forEach(p -> out.addAll(p.blocks()));
        return out;
    }
}
