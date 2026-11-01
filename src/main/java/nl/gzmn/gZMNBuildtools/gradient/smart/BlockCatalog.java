package nl.gzmn.gZMNBuildtools.gradient.smart;

import nl.gzmn.gZMNBuildtools.palette.BlockColors;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Knows, for every block id in the game, its shape (full, stairs, slab, wall,
 * fence, fence gate) and the base material it belongs to, by deriving variant
 * relationships from the complete block list (e.g. {@code stone_brick_stairs}
 * belongs to {@code stone_bricks}, {@code oak_fence} to {@code oak_planks}).
 */
public final class BlockCatalog {

    /** Classification of one block. */
    public record Info(String id, ShapeKind kind, String base, String group) {
    }

    private static final List<String> TEXTURE_PREFIXES = List.of("mossy_", "cracked_", "chiseled_");

    private final Set<String> ids;
    private final Predicate<String> fullCube;
    private final Map<String, Info> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, EnumMap<ShapeKind, String>> variants = new HashMap<>();

    /**
     * @param ids      every block id (no namespace)
     * @param fullCube whether a block is a full cube; ids without a variant
     *                 suffix that are not full cubes are {@link ShapeKind#OTHER}
     */
    public BlockCatalog(Set<String> ids, Predicate<String> fullCube) {
        this.ids = Set.copyOf(ids);
        this.fullCube = fullCube;
        for (String id : this.ids) {
            ShapeKind.fromSuffix(id).ifPresent(kind -> {
                String base = deriveBase(id, kind);
                if (base != null) {
                    variants.computeIfAbsent(base, b -> new EnumMap<>(ShapeKind.class)).put(kind, id);
                }
            });
        }
    }

    private String deriveBase(String id, ShapeKind kind) {
        String stem = id.substring(0, id.length() - kind.suffix().length());
        for (String candidate : List.of(stem, stem + "s", stem + "_planks", stem + "_block")) {
            if (ids.contains(candidate) && ShapeKind.fromSuffix(candidate).isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    public boolean exists(String id) {
        return ids.contains(BlockColors.normalize(id));
    }

    public Set<String> ids() {
        return ids;
    }

    public Info classify(String rawId) {
        String id = BlockColors.normalize(rawId);
        return cache.computeIfAbsent(id, this::computeInfo);
    }

    private Info computeInfo(String id) {
        Optional<ShapeKind> suffixKind = ShapeKind.fromSuffix(id);
        if (suffixKind.isPresent()) {
            String base = deriveBase(id, suffixKind.get());
            return new Info(id, suffixKind.get(), base, group(base != null ? base : id));
        }
        ShapeKind kind = fullCube.test(id) ? ShapeKind.FULL : ShapeKind.OTHER;
        return new Info(id, kind, id, group(id));
    }

    /**
     * Material group used to decide what counts as "the same material":
     * texture variants (mossy/cracked/chiseled) join their plain block.
     */
    private String group(String base) {
        for (String prefix : TEXTURE_PREFIXES) {
            if (base.startsWith(prefix)) {
                String plain = base.substring(prefix.length());
                if (ids.contains(plain)) {
                    return plain;
                }
                // chiseled_stone_bricks -> stone_bricks, chiseled_sandstone -> sandstone
                if (ids.contains(plain + "s")) {
                    return plain + "s";
                }
            }
        }
        return base;
    }

    /** The block of {@code kind} made of {@code base}, if the game has one. */
    public Optional<String> variant(String base, ShapeKind kind) {
        String b = BlockColors.normalize(base);
        if (kind == ShapeKind.FULL) {
            return ids.contains(b) ? Optional.of(b) : Optional.empty();
        }
        EnumMap<ShapeKind, String> map = variants.get(b);
        return map == null ? Optional.empty() : Optional.ofNullable(map.get(kind));
    }

    /** All base materials that have a {@code kind} variant. */
    public Set<String> basesWith(ShapeKind kind) {
        Set<String> out = new HashSet<>();
        variants.forEach((base, map) -> {
            if (map.containsKey(kind)) {
                out.add(base);
            }
        });
        return Collections.unmodifiableSet(out);
    }
}
