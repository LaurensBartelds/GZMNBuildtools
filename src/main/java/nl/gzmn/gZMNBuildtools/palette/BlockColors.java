package nl.gzmn.gZMNBuildtools.palette;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Average colours of every block, loaded from the bundled
 * {@code block-colors.tsv} (generated from the game's textures by
 * {@code tools/generate_block_colors.py}).
 */
public final class BlockColors {

    /** Colour data for one block id (without the {@code minecraft:} prefix). */
    public record Entry(String id, int sideRgb, int topRgb, boolean tinted, boolean transparent) {
        public OkLab side() {
            return OkLab.fromRgb(sideRgb);
        }

        public OkLab top() {
            return OkLab.fromRgb(topRgb);
        }
    }

    private final Map<String, Entry> entries;

    private BlockColors(Map<String, Entry> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }

    public static BlockColors load(InputStream in) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 4) {
                    continue;
                }
                entries.put(parts[0], new Entry(parts[0], Integer.parseInt(parts[1], 16),
                        Integer.parseInt(parts[2], 16), parts[3].contains("t"), parts[3].contains("a")));
            }
        }
        return new BlockColors(entries);
    }

    /** Loads the table bundled in the plugin jar. */
    public static BlockColors bundled() {
        try (InputStream in = BlockColors.class.getClassLoader().getResourceAsStream("block-colors.tsv")) {
            if (in == null) {
                throw new IllegalStateException("block-colors.tsv is missing from the plugin jar");
            }
            return load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read block-colors.tsv", e);
        }
    }

    public static String normalize(String id) {
        String lower = id.trim().toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("minecraft:") ? lower.substring("minecraft:".length()) : lower;
    }

    public Optional<Entry> get(String id) {
        return Optional.ofNullable(entries.get(normalize(id)));
    }

    public Optional<OkLab> side(String id) {
        return get(id).map(Entry::side);
    }

    public Set<String> ids() {
        return entries.keySet();
    }

    public int size() {
        return entries.size();
    }
}
