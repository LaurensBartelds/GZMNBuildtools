package nl.gzmn.gZMNBuildtools.common;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Message translations keyed by the English template used in the code. Files
 * live in {@code lang/<language>.yml}; each line maps an English template to
 * its translation. A player sees their game language when there is a file
 * for it, and English otherwise.
 */
public final class Translations {

    /** The languages shipped with the plugin. */
    public static final List<String> BUNDLED = List.of("en", "nl");
    /** YAML keys are whole sentences full of dots, so paths use a character that never appears in them. */
    private static final char SEPARATOR = '\u001F';

    public static final Translations NONE = new Translations(Map.of(), null);

    private final Map<String, Map<String, String>> languages;
    private final String forced;

    public Translations(Map<String, Map<String, String>> languages, String forced) {
        this.languages = languages;
        this.forced = forced;
    }

    /**
     * Loads the bundled languages (copying them to {@code lang/} on first run)
     * and every {@code lang/*.yml} in the data folder; edited files win over
     * the bundled text, key by key.
     *
     * @param setting "auto" for each player's own language, or a language code for everyone
     */
    public static Translations load(JavaPlugin plugin, String setting) {
        Logger log = plugin.getLogger();
        File folder = new File(plugin.getDataFolder(), "lang");
        Map<String, Map<String, String>> languages = new HashMap<>();
        for (String lang : BUNDLED) {
            String resource = "lang/" + lang + ".yml";
            if (!new File(folder, lang + ".yml").exists() && plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            }
            try (InputStream in = plugin.getResource(resource)) {
                if (in != null) {
                    languages.put(lang, new HashMap<>(read(new InputStreamReader(in, StandardCharsets.UTF_8))));
                }
            } catch (IOException | InvalidConfigurationException e) {
                log.warning("Bundled " + resource + " could not be read: " + e.getMessage());
            }
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String lang = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                try {
                    Map<String, String> table = read(new StringReader(Files.readString(file.toPath())));
                    languages.computeIfAbsent(lang, k -> new HashMap<>()).putAll(table);
                } catch (IOException | InvalidConfigurationException e) {
                    log.warning("lang/" + file.getName() + " could not be read: " + e.getMessage());
                }
            }
        }
        String forced = setting == null || setting.isBlank() || setting.equalsIgnoreCase("auto") ? null
                : setting.toLowerCase(Locale.ROOT);
        if (forced != null && !languages.containsKey(forced)) {
            log.warning("messages.language is '" + forced + "' but there is no lang/" + forced
                    + ".yml; using each player's own language.");
            forced = null;
        }
        Map<String, Map<String, String>> frozen = new HashMap<>();
        languages.forEach((k, v) -> frozen.put(k, Map.copyOf(v)));
        return new Translations(Collections.unmodifiableMap(frozen), forced);
    }

    /** Reads one language file: English template -> translation. */
    public static Map<String, String> read(Reader reader) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(SEPARATOR);
        yaml.load(reader);
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            String value = yaml.getString(key);
            if (value != null && !value.isEmpty()) {
                out.put(key, value);
            }
        }
        return out;
    }

    /** The text to use for {@code template} in {@code locale} (the template itself when untranslated). */
    public String translate(String template, Locale locale) {
        String lang = forced != null ? forced : locale == null ? "en" : locale.getLanguage().toLowerCase(Locale.ROOT);
        Map<String, String> table = languages.get(lang);
        String text = table == null ? null : table.get(template);
        if (text == null && !lang.equals("en")) {
            Map<String, String> english = languages.get("en");
            text = english == null ? null : english.get(template);
        }
        return text == null ? template : text;
    }

    public Set<String> languages() {
        return languages.keySet();
    }

    public int size(String language) {
        Map<String, String> table = languages.get(language);
        return table == null ? 0 : table.size();
    }
}
