package nl.gzmn.gZMNBuildtools.integration.worldedit.parser;

import com.sk89q.worldedit.extension.input.InputParseException;

import java.util.Locale;
import java.util.stream.Stream;

/** Small helpers for parsing the bracketed arguments of rich masks/patterns. */
public final class ParserArgs {

    private ParserArgs() {
    }

    public static double number(String raw, String name) throws InputParseException {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith("d")) {
            value = value.substring(0, value.length() - 1);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new InputParseException("Expected a number for " + name + " but got '" + raw + "'");
        }
    }

    public static int integer(String raw, String name, int min, int max) throws InputParseException {
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InputParseException("Expected a whole number for " + name + " but got '" + raw + "'");
        }
        if (value < min || value > max) {
            throw new InputParseException(name + " must be between " + min + " and " + max + " (got " + value + ")");
        }
        return value;
    }

    public static InputParseException usage(String syntax) {
        return new InputParseException("Usage: " + syntax);
    }

    /** Filters a fixed list of suggestions by the current partial input. */
    public static Stream<String> suggest(String input, String... options) {
        String lower = input.toLowerCase(Locale.ROOT);
        return Stream.of(options).filter(o -> o.startsWith(lower));
    }
}
