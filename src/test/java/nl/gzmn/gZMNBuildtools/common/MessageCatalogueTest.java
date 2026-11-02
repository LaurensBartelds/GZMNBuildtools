package nl.gzmn.gZMNBuildtools.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Keeps {@code lang/en.yml} in step with the messages in the code, and checks
 * every translation against it. After changing messages, regenerate the
 * catalogue with {@code mvn test -Dcatalogue.write=true}.
 */
class MessageCatalogueTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path LANG = Path.of("src/main/resources/lang");
    private static final Pattern CALL = Pattern.compile("\\bmessages\\.(info|success|warn|error|verbose|text)\\(");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?([-#+ 0,(]*\\d*(?:\\.\\d+)?)([a-zA-Z%])");
    private static final String HEADER = """
            # GZMNBuildtools messages in English: each key is the message as written in
            # the code, and the value is the text players see. This file is generated
            # from the code, but you can change the values to reword messages.
            #
            # To translate, copy this file to <language>.yml (e.g. de.yml) and translate
            # the values. Keep the placeholders (%s, %d, %.2f): to change their order,
            # number them (%2$s). Players see their game's language when a file exists
            # for it (or set messages.language in config.yml). Run /gzmnbuildtools reload
            # after editing.
            """;

    /** Every fixed template passed to the Messages methods in the code. */
    static Set<String> templates() throws IOException {
        Set<String> out = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = CALL.matcher(src);
                while (m.find()) {
                    int pos = skipArgument(src, m.end());
                    if (pos < 0) {
                        continue;
                    }
                    StringBuilder template = new StringBuilder();
                    boolean any = false;
                    while (true) {
                        pos = skipSpace(src, pos);
                        if (pos >= src.length() || src.charAt(pos) != '"') {
                            break;
                        }
                        int end = literalEnd(src, pos);
                        template.append(unescape(src.substring(pos + 1, end)));
                        any = true;
                        pos = skipSpace(src, end + 1);
                        if (pos < src.length() && src.charAt(pos) == '+') {
                            int next = skipSpace(src, pos + 1);
                            if (next < src.length() && src.charAt(next) == '"') {
                                pos = next;
                                continue;
                            }
                            any = false; // joined with something computed: not a fixed template
                        }
                        break;
                    }
                    if (any && !template.toString().equals("%s")) {
                        out.add(template.toString());
                    }
                }
            }
        }
        return out;
    }

    /** Skips the first argument (the player) and its comma; -1 when there is no second argument. */
    private static int skipArgument(String src, int pos) {
        int depth = 0;
        for (int i = pos; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '"') {
                i = literalEnd(src, i);
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return -1;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                return i + 1;
            }
        }
        return -1;
    }

    private static int skipSpace(String src, int pos) {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int literalEnd(String src, int start) {
        for (int i = start + 1; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        throw new IllegalStateException("Unterminated string literal");
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                out.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'u' -> {
                    out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> out.append(n);
            }
        }
        return out.toString();
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + '"';
    }

    static String render(Set<String> templates) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (String t : templates) {
            sb.append(quote(t)).append(": ").append(quote(t)).append('\n');
        }
        return sb.toString();
    }

    /** The placeholders of a template as (argument index, conversion), in index order. */
    static List<String> placeholders(String template) {
        List<String> out = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);
        int implicit = 0;
        while (m.find()) {
            String conversion = m.group(2) + m.group(3);
            if (m.group(3).equals("%") || m.group(3).equals("n")) {
                continue;
            }
            int index = m.group(1) != null ? Integer.parseInt(m.group(1)) : ++implicit;
            out.add(index + ":" + conversion.toLowerCase(Locale.ROOT));
        }
        out.sort(null);
        return out;
    }

    @Test
    void englishCatalogueMatchesTheCode() throws IOException {
        Set<String> templates = templates();
        assertTrue(templates.size() > 100, "found " + templates.size() + " templates");
        Path file = LANG.resolve("en.yml");
        String expected = render(templates);
        String actual = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        if (expected.equals(actual)) {
            return;
        }
        if (Boolean.getBoolean("catalogue.write")) {
            Files.createDirectories(LANG);
            Files.writeString(file, expected, StandardCharsets.UTF_8);
            return;
        }
        Set<String> previous = Files.exists(file) ? read(file).keySet() : Set.of();
        Set<String> added = new TreeSet<>(templates);
        added.removeAll(previous);
        Set<String> removed = new TreeSet<>(previous);
        removed.removeAll(templates);
        fail("lang/en.yml is out of date; run mvn test -Dcatalogue.write=true. New: " + added + " Gone: " + removed);
    }

    @Test
    void translationsFitTheCatalogue() throws IOException {
        Set<String> templates = templates();
        try (Stream<Path> files = Files.list(LANG)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")).toList()) {
                String lang = file.getFileName().toString().replace(".yml", "");
                if (lang.equals("en")) {
                    continue;
                }
                Map<String, String> table = read(file);
                Set<String> stale = new TreeSet<>(table.keySet());
                stale.removeAll(templates);
                assertEquals(Set.of(), stale, lang + ".yml translates messages the code no longer has");
                Set<String> missing = new TreeSet<>(templates);
                missing.removeAll(table.keySet());
                assertEquals(Set.of(), missing, lang + ".yml is missing translations");
                table.forEach((key, value) -> assertEquals(placeholders(key), placeholders(value),
                        lang + ".yml: placeholders differ for \"" + key + "\""));
            }
        }
    }

    @Test
    void picksThePlayersLanguageAndFallsBack() {
        Translations t = new Translations(Map.of(
                "nl", Map.of("Placed %d blocks.", "%d blokken geplaatst."),
                "en", Map.of("Placed %d blocks.", "Put down %d blocks.", "Hello", "Hi")), null);
        assertEquals("%d blokken geplaatst.", t.translate("Placed %d blocks.", Locale.of("nl", "NL")));
        assertEquals("Hi", t.translate("Hello", Locale.of("nl", "BE")), "missing in nl: the English file");
        assertEquals("Put down %d blocks.", t.translate("Placed %d blocks.", Locale.GERMANY), "no German file");
        assertEquals("Unknown", t.translate("Unknown", Locale.of("nl")));
        Translations forced = new Translations(t.languages().stream().collect(java.util.stream.Collectors.toMap(
                l -> l, l -> l.equals("nl") ? Map.of("Hello", "Hallo") : Map.<String, String>of())), "nl");
        assertEquals("Hallo", forced.translate("Hello", Locale.US));
    }

    @Test
    void wrongPlaceholdersFallBackToEnglish() {
        AdventureMessages messages = new AdventureMessages(false);
        messages.setTranslations(new Translations(Map.of("en", Map.of("Placed %d blocks.", "Placed %s %s blocks.")),
                null));
        assertEquals("Placed 3 blocks.", messages.text(null, "Placed %d blocks.", 3));
        assertEquals(List.of("1:d", "2:s"), placeholders("%d of %s"));
        assertEquals(List.of("1:d", "2:s"), placeholders("%2$s heeft %1$d"));
        assertEquals(List.of("1:.2f"), placeholders("scale=%.2f (100%%)"));
    }

    private static Map<String, String> read(Path file) throws IOException {
        try {
            return Translations.read(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IOException(file + ": " + e.getMessage(), e);
        }
    }
}
