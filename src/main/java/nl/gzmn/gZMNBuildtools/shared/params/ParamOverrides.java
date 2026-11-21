package nl.gzmn.gZMNBuildtools.shared.params;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-wide overrides for tool parameters, read from the {@code params:}
 * section of config.yml and applied on top of the built-in parameters:
 *
 * <pre>
 * params:
 *   roof:                      # a tool with one parameter set (key = its id)
 *     overhang: { default: 2, max: 16 }
 *   terrain-brushes:           # a tool with one set per brush/shape
 *     smooth:
 *       kernel: { default: 3, min: 1, max: 8 }
 * </pre>
 *
 * Each entry may set {@code default}, {@code min} and {@code max}; a choice
 * parameter's {@code default} may be given as the choice name. Parsing, default
 * values, menus and validation all see the overridden values. Reloaded with
 * {@code /gzmnbuildtools reload}.
 */
public final class ParamOverrides {

    private record Override(Object def, Double min, Double max) {
    }

    private static volatile Map<String, Map<String, Override>> bySpec = Map.of();

    private ParamOverrides() {
    }

    /** Reads the {@code params:} section; returns problems to report in the console. */
    public static List<String> load(ConfigurationSection config) {
        List<String> problems = new ArrayList<>();
        Map<String, Map<String, Override>> out = new LinkedHashMap<>();
        ConfigurationSection params = config.getConfigurationSection("params");
        if (params != null) {
            for (String group : params.getKeys(false)) {
                ConfigurationSection groupSection = params.getConfigurationSection(group);
                if (groupSection == null) {
                    continue;
                }
                for (String key : groupSection.getKeys(false)) {
                    ConfigurationSection child = groupSection.getConfigurationSection(key);
                    if (child == null) {
                        continue;
                    }
                    if (isOverride(child)) {
                        put(out, group, key, child, problems);
                    } else {
                        for (String param : child.getKeys(false)) {
                            ConfigurationSection paramSection = child.getConfigurationSection(param);
                            if (paramSection != null && isOverride(paramSection)) {
                                put(out, group + "." + key, param, paramSection, problems);
                            }
                        }
                    }
                }
            }
        }
        bySpec = Map.copyOf(out);
        return problems;
    }

    private static boolean isOverride(ConfigurationSection section) {
        return section.isSet("default") || section.isSet("min") || section.isSet("max");
    }

    private static void put(Map<String, Map<String, Override>> out, String specKey, String param,
                            ConfigurationSection section, List<String> problems) {
        Object def = section.isSet("default") ? section.get("default") : null;
        if (def != null && !(def instanceof Number) && !(def instanceof String)) {
            problems.add("params." + specKey + "." + param + ".default: must be a number or a choice name");
            def = null;
        }
        Double min = number(section, "min", specKey, param, problems);
        Double max = number(section, "max", specKey, param, problems);
        out.computeIfAbsent(specKey, k -> new LinkedHashMap<>()).put(param, new Override(def, min, max));
    }

    private static Double number(ConfigurationSection section, String key, String specKey, String param,
                                 List<String> problems) {
        if (!section.isSet(key)) {
            return null;
        }
        double value = section.getDouble(key, Double.NaN);
        if (Double.isNaN(value)) {
            problems.add("params." + specKey + "." + param + "." + key + ": must be a number");
            return null;
        }
        return value;
    }

    /** The spec's parameters with the configured overrides applied (the built-in list when there are none). */
    public static List<BrushParam> apply(String specKey, List<BrushParam> base) {
        Map<String, Override> spec = bySpec.get(specKey);
        if (spec == null || spec.isEmpty()) {
            return base;
        }
        List<BrushParam> out = new ArrayList<>(base.size());
        for (BrushParam p : base) {
            Override o = spec.get(p.name());
            if (o == null) {
                out.add(p);
                continue;
            }
            double min = o.min == null ? p.min() : o.min;
            double max = o.max == null ? p.max() : o.max;
            if (max < min) {
                double t = min;
                min = max;
                max = t;
            }
            double def = p.defaultValue();
            if (o.def instanceof Number n) {
                def = n.doubleValue();
            } else if (o.def instanceof String s) {
                if (p.isChoice()) {
                    int index = p.choices().indexOf(s.toLowerCase(Locale.ROOT));
                    if (index >= 0) {
                        def = index;
                    }
                } else {
                    try {
                        def = Double.parseDouble(s.trim());
                    } catch (NumberFormatException ignored) {
                        // Keep the built-in default for a value that does not parse.
                    }
                }
            }
            def = Math.max(min, Math.min(max, def));
            out.add(new BrushParam(p.name(), def, min, max, p.description(), p.choices()));
        }
        return List.copyOf(out);
    }
}
