package nl.gzmn.gZMNBuildtools.shared.build;

import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.shared.palette.MaterialClass;
import nl.gzmn.gZMNBuildtools.shared.palette.OkLab;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the block to place when a palette material replaces a block of a given
 * shape: the material's own stairs/slab/wall/... when it has one, otherwise the
 * closest-coloured material that does.
 */
public final class VariantResolver {

    private final BlockCatalog catalog;
    private final BlockColors colors;
    private final Map<String, Optional<String>> fallbacks = new ConcurrentHashMap<>();

    public VariantResolver(BlockCatalog catalog, BlockColors colors) {
        this.catalog = catalog;
        this.colors = colors;
    }

    public Optional<String> resolve(String base, ShapeKind kind) {
        if (kind == ShapeKind.OTHER) {
            return Optional.empty();
        }
        Optional<String> own = catalog.variant(base, kind);
        if (own.isPresent()) {
            return own;
        }
        return fallbacks.computeIfAbsent(BlockColors.normalize(base) + "|" + kind, k -> closest(base, kind));
    }

    private Optional<String> closest(String base, ShapeKind kind) {
        String id = BlockColors.normalize(base);
        Optional<OkLab> colour = colors.side(id);
        if (colour.isEmpty()) {
            return Optional.empty();
        }
        OkLab target = colour.get();
        MaterialClass material = MaterialClass.of(id);
        Set<String> words = Set.of(id.split("_"));
        return catalog.basesWith(kind).stream()
                .filter(b -> colors.side(b).isPresent())
                .min(Comparator.comparingDouble(b -> fallbackCost(b, target, material, words)))
                .flatMap(b -> catalog.variant(b, kind));
    }

    /**
     * Colour distance with hue mismatches counted extra, a penalty for a
     * different material class and a bonus for a related name (smooth_stone
     * prefers stone_stairs over a same-grey block of another family).
     */
    private double fallbackCost(String candidate, OkLab target, MaterialClass material, Set<String> words) {
        OkLab c = colors.side(candidate).orElseThrow();
        double cost = c.distance(target) + 1.5 * c.chromaDistance(target);
        if (MaterialClass.of(candidate) != material) {
            cost += 0.05;
        }
        for (String word : candidate.split("_")) {
            if (words.contains(word) && !word.equals("block")) {
                cost -= 0.03;
                break;
            }
        }
        return cost;
    }
}
