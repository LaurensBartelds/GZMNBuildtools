package nl.gzmn.gZMNBuildtools.gradient.smart;

import nl.gzmn.gZMNBuildtools.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.palette.OkLab;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
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
        Optional<OkLab> colour = colors.side(base);
        if (colour.isEmpty()) {
            return Optional.empty();
        }
        OkLab target = colour.get();
        return catalog.basesWith(kind).stream()
                .filter(b -> colors.side(b).isPresent())
                .min(Comparator.comparingDouble(b -> colors.side(b).get().distance(target)))
                .flatMap(b -> catalog.variant(b, kind));
    }
}
