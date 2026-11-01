package nl.gzmn.gZMNBuildtools.brush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parsed arguments of a brush or generator: its fill patterns followed by
 * parameters given as {@code name=value} or positionally in declaration order.
 */
public final class BrushArguments<T extends ParamSpec> {

    private final T type;
    private final List<String> patterns;
    private final Map<String, Double> values;

    private BrushArguments(T type, List<String> patterns, Map<String, Double> values) {
        this.type = type;
        this.patterns = List.copyOf(patterns);
        this.values = values;
    }

    public static <T extends ParamSpec> BrushArguments<T> parse(T type, String input) {
        String[] tokens = input == null || input.isBlank() ? new String[0] : input.trim().split("\\s+");
        int index = 0;
        List<String> patterns = new ArrayList<>();
        for (String name : type.patternNames()) {
            if (index >= tokens.length) {
                throw new IllegalArgumentException("Missing " + name + " pattern. Usage: " + type.usage());
            }
            patterns.add(tokens[index++]);
        }
        Map<String, Double> values = new HashMap<>();
        int positional = 0;
        for (; index < tokens.length; index++) {
            String token = tokens[index];
            int eq = token.indexOf('=');
            BrushParam param;
            String raw;
            if (eq > 0) {
                String name = token.substring(0, eq).toLowerCase(Locale.ROOT);
                param = type.params().stream().filter(p -> p.name().equals(name)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown parameter '" + name + "' for "
                                + type.id() + ". Usage: " + type.usage()));
                raw = token.substring(eq + 1);
            } else {
                if (positional >= type.params().size()) {
                    throw new IllegalArgumentException("Too many arguments. Usage: " + type.usage());
                }
                param = type.params().get(positional++);
                raw = token;
            }
            values.put(param.name(), parseValue(param, raw));
        }
        return new BrushArguments<>(type, patterns, values);
    }

    private static double parseValue(BrushParam param, String raw) {
        if (param.isChoice()) {
            int index = param.choices().indexOf(raw.toLowerCase(Locale.ROOT));
            if (index < 0) {
                throw new IllegalArgumentException(param.name() + " must be one of " + String.join(", ", param.choices()));
            }
            return index;
        }
        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a number for " + param.name());
        }
        if (value < param.min() || value > param.max()) {
            throw new IllegalArgumentException(param.name() + " must be between "
                    + ParamSpec.format(param.min()) + " and " + ParamSpec.format(param.max()));
        }
        return value;
    }

    public T type() {
        return type;
    }

    /** The first fill pattern text, or null for brushes that reshape terrain. */
    public String pattern() {
        return patterns.isEmpty() ? null : patterns.get(0);
    }

    public List<String> patterns() {
        return patterns;
    }

    public double get(String name) {
        Double value = values.get(name);
        if (value != null) {
            return value;
        }
        return param(name).defaultValue();
    }

    public int getInt(String name) {
        return (int) Math.round(get(name));
    }

    /** The chosen option name of a choice parameter. */
    public String choice(String name) {
        return param(name).display(get(name));
    }

    private BrushParam param(String name) {
        return type.params().stream().filter(p -> p.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No parameter " + name));
    }

    /** Human readable summary of the effective parameter values. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (BrushParam p : type.params()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(p.name()).append('=').append(p.display(get(p.name())));
        }
        return sb.toString();
    }
}
