package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** A named terrain look: rules tried top to bottom, the first match decides the block. */
public record TexturePreset(String id, String description, List<TextureRule> rules) {

    /** How deep below the surface the preset reaches. */
    public int maxDepth() {
        return (int) Math.min(64, rules.stream().mapToDouble(r -> r.depth().max()).max().orElse(0));
    }

    /** Every block the preset can place. */
    public Set<String> blocks() {
        Set<String> out = new LinkedHashSet<>();
        for (TextureRule rule : rules) {
            rule.place().forEach(w -> out.add(w.block()));
            out.addAll(rule.bands());
        }
        return out;
    }

    public TextureRule match(int depth, double slope, int y, double noise) {
        for (TextureRule rule : rules) {
            if (rule.matches(depth, slope, y, noise)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Reads a preset from its YAML map form:
     * {@code {description: ..., rules: [{when: {depth: "0..1", slope: "40.."}, place: ["60%stone", "andesite"]}]}}.
     *
     * @throws IllegalArgumentException with a message naming the problem
     */
    public static TexturePreset fromMap(String id, Map<?, ?> map, Predicate<String> isBlock) {
        Object description = map.get("description");
        Object rawRules = map.get("rules");
        if (!(rawRules instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("preset '" + id + "' has no rules");
        }
        List<TextureRule> rules = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> rule)) {
                throw new IllegalArgumentException("preset '" + id + "': each rule must be a map");
            }
            Map<?, ?> when = rule.get("when") instanceof Map<?, ?> w ? w : Map.of();
            List<TextureRule.Weighted> place = new ArrayList<>();
            Object rawPlace = rule.get("place");
            for (Object p : rawPlace instanceof List<?> l ? l : rawPlace == null ? List.of() : List.of(rawPlace)) {
                place.add(weighted(p.toString(), id, isBlock));
            }
            List<String> bands = new ArrayList<>();
            if (rule.get("bands") instanceof List<?> b) {
                for (Object band : b) {
                    bands.add(block(band.toString(), id, isBlock));
                }
            }
            if (place.isEmpty() && bands.isEmpty()) {
                throw new IllegalArgumentException("preset '" + id + "': a rule needs place or bands");
            }
            int bandHeight = rule.get("band-height") instanceof Number n ? n.intValue() : 2;
            rules.add(new TextureRule(range(when, "depth", new Range(0, 0)), range(when, "slope", Range.ANY),
                    range(when, "height", Range.ANY), range(when, "noise", Range.ANY), place, bands, bandHeight));
        }
        return new TexturePreset(id, description == null ? "" : description.toString(), rules);
    }

    private static Range range(Map<?, ?> when, String key, Range fallback) {
        Object value = when.get(key);
        return value == null ? fallback : Range.parse(value.toString());
    }

    private static TextureRule.Weighted weighted(String text, String preset, Predicate<String> isBlock) {
        int pct = text.indexOf('%');
        if (pct < 0) {
            return new TextureRule.Weighted(block(text, preset, isBlock), 1);
        }
        try {
            return new TextureRule.Weighted(block(text.substring(pct + 1), preset, isBlock),
                    Double.parseDouble(text.substring(0, pct).trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("preset '" + preset + "': bad weight in '" + text + "'");
        }
    }

    private static String block(String text, String preset, Predicate<String> isBlock) {
        String id = BlockColors.normalize(text);
        if (!isBlock.test(id)) {
            throw new IllegalArgumentException("preset '" + preset + "': unknown block '" + id + "'");
        }
        return id;
    }
}
