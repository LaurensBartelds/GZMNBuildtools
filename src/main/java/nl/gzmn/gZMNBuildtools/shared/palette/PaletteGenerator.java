package nl.gzmn.gZMNBuildtools.shared.palette;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds block gradients by colour: shades of one block, a ramp between two
 * blocks, filling in between a few key blocks, and "aged" ramps that start with
 * the mossy/cracked versions of a material.
 */
public final class PaletteGenerator {

    /** Lightness change per block in a shade ramp, and the most a ramp may span either side of its base. */
    private static final double SHADE_STEP = 0.04;
    private static final double MAX_SHADE_SPREAD = 0.3;

    /** Extra colour distance charged for leaving the material class of the endpoints. */
    private static final double MATERIAL_PENALTY = 0.04;

    private final BlockColors colors;
    private final List<String> candidates;

    /**
     * @param eligible which blocks may appear in generated palettes (full,
     *                 opaque, non-functional building blocks)
     */
    public PaletteGenerator(BlockColors colors, Predicate<String> eligible) {
        this.colors = colors;
        this.candidates = colors.ids().stream()
                .filter(eligible)
                .filter(id -> colors.get(id).map(e -> !e.transparent() && !e.tinted()).orElse(false))
                .sorted()
                .toList();
    }

    public List<String> candidates() {
        return candidates;
    }

    public boolean isCandidate(String id) {
        return candidates.contains(BlockColors.normalize(id));
    }

    private OkLab colour(String id) {
        return colors.side(id).orElseThrow(() -> new IllegalArgumentException("Unknown block colour: " + id));
    }

    /** The eligible block closest in colour to {@code target}, skipping {@code exclude}. */
    public Optional<String> nearest(OkLab target, Set<String> exclude) {
        return candidates.stream()
                .filter(id -> !exclude.contains(id))
                .min(Comparator.comparingDouble(id -> colour(id).distance(target)));
    }

    /**
     * {@code count} blocks with a similar hue to {@code base}, ordered dark to
     * light, always including {@code base} itself.
     */
    public List<String> shades(String base, int count) {
        String baseId = BlockColors.normalize(base);
        OkLab b = colour(baseId);
        if (count <= 1) {
            return List.of(baseId);
        }

        MaterialClass material = MaterialClass.of(baseId);
        List<String> similar = List.of();
        for (double tolerance = 0.025; tolerance <= 0.16; tolerance *= 1.4) {
            final double tol = tolerance;
            List<String> matching = candidates.stream().filter(id -> colour(id).chromaDistance(b) <= tol).toList();
            List<String> sameMaterial = matching.stream().filter(id -> MaterialClass.of(id) == material).toList();
            similar = sameMaterial.size() >= count ? sameMaterial : matching;
            if (sameMaterial.size() >= count * 2) {
                break;
            }
        }
        List<String> pool = new ArrayList<>(similar);
        if (!pool.contains(baseId)) {
            pool.add(baseId);
        }

        double minL = pool.stream().mapToDouble(id -> colour(id).l()).min().orElse(b.l());
        double maxL = pool.stream().mapToDouble(id -> colour(id).l()).max().orElse(b.l());
        double spread = Math.min(MAX_SHADE_SPREAD, SHADE_STEP * (count - 1) / 2.0 + SHADE_STEP);
        double low = Math.max(minL, b.l() - spread);
        double high = Math.min(maxL, b.l() + spread);

        Set<String> chosen = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            double target = low + (high - low) * i / (count - 1);
            pool.stream()
                    .filter(id -> !chosen.contains(id))
                    .min(Comparator.comparingDouble(id ->
                            Math.abs(colour(id).l() - target) + 0.6 * colour(id).chromaDistance(b)))
                    .ifPresent(chosen::add);
        }
        if (!chosen.contains(baseId)) {
            String closest = chosen.stream()
                    .min(Comparator.comparingDouble(id -> Math.abs(colour(id).l() - b.l())))
                    .orElse(null);
            chosen.remove(closest);
            chosen.add(baseId);
        }
        return chosen.stream().sorted(Comparator.comparingDouble(id -> colour(id).l())).toList();
    }

    /** {@code count} blocks forming an even colour ramp from {@code from} to {@code to}. */
    public List<String> between(String from, String to, int count) {
        String start = BlockColors.normalize(from);
        String end = BlockColors.normalize(to);
        if (count <= 2) {
            return start.equals(end) ? List.of(start) : List.of(start, end);
        }
        OkLab a = colour(start);
        OkLab z = colour(end);
        List<String> result = new ArrayList<>();
        Set<String> used = new HashSet<>(List.of(start, end));
        result.add(start);
        Set<MaterialClass> materials = EnumSet.of(MaterialClass.of(start), MaterialClass.of(end));
        for (int i = 1; i < count - 1; i++) {
            OkLab target = a.lerp(z, i / (double) (count - 1));
            candidates.stream()
                    .filter(id -> !used.contains(id))
                    .min(Comparator.comparingDouble(id -> colour(id).distance(target)
                            + (materials.contains(MaterialClass.of(id)) ? 0 : MATERIAL_PENALTY)))
                    .ifPresent(id -> {
                        // Skip a pick that is not actually closer to this step than an endpoint is.
                        if (colour(id).distance(target) < Math.min(a.distance(target), z.distance(target))) {
                            used.add(id);
                            result.add(id);
                        }
                    });
        }
        result.add(end);
        return result;
    }

    /**
     * Expands a few key blocks into a smooth gradient of about {@code count}
     * blocks, spending the extra steps on the segments with the biggest colour jumps.
     */
    public List<String> fill(List<String> keys, int count) {
        List<String> ids = keys.stream().map(BlockColors::normalize).toList();
        if (ids.size() < 2 || count <= ids.size()) {
            return ids;
        }
        int extra = count - ids.size();
        double[] gaps = new double[ids.size() - 1];
        double total = 0;
        for (int i = 0; i < gaps.length; i++) {
            gaps[i] = colour(ids.get(i)).distance(colour(ids.get(i + 1)));
            total += gaps[i];
        }
        int[] slots = new int[gaps.length];
        int assigned = 0;
        for (int i = 0; i < gaps.length; i++) {
            slots[i] = total == 0 ? 0 : (int) Math.floor(extra * gaps[i] / total);
            assigned += slots[i];
        }
        // Hand out rounding leftovers to the largest gaps.
        while (assigned < extra) {
            int best = 0;
            for (int i = 1; i < gaps.length; i++) {
                if (gaps[i] / (slots[i] + 1) > gaps[best] / (slots[best] + 1)) {
                    best = i;
                }
            }
            slots[best]++;
            assigned++;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < gaps.length; i++) {
            List<String> segment = between(ids.get(i), ids.get(i + 1), slots[i] + 2);
            for (int j = (i == 0 ? 0 : 1); j < segment.size(); j++) {
                if (result.isEmpty() || !result.get(result.size() - 1).equals(segment.get(j))) {
                    result.add(segment.get(j));
                }
            }
        }
        return result;
    }

    /**
     * A weathered ramp for building faces: the mossy and cracked versions of
     * {@code base} at the bottom (when they exist), then shades up to lighter tones.
     */
    public List<String> aged(String base, int count) {
        String baseId = BlockColors.normalize(base);
        List<String> worn = new ArrayList<>();
        for (String prefix : List.of("mossy_", "cracked_")) {
            String variant = prefix + baseId;
            if (colors.get(variant).isPresent()) {
                worn.add(variant);
            }
        }
        if (worn.isEmpty()) {
            return shades(baseId, count);
        }
        int need = Math.max(1, count - worn.size());
        // A ramp centred on the base; its lighter half (base first) tops the worn blocks.
        double baseL = colour(baseId).l();
        List<String> lighter = shades(baseId, Math.max(2, need * 2 - 1)).stream()
                .filter(id -> colour(id).l() >= baseL - 1e-9)
                .filter(id -> !worn.contains(id))
                .limit(need)
                .toList();
        List<String> result = new ArrayList<>(worn);
        result.addAll(lighter);
        return result;
    }

    public BlockColors colors() {
        return colors;
    }
}
