package nl.gzmn.gZMNBuildtools.shared.build;

import java.util.Locale;
import java.util.Optional;

/** The shape of a building block; repainting keeps it. */
public enum ShapeKind {
    FULL(""),
    STAIRS("_stairs"),
    SLAB("_slab"),
    WALL("_wall"),
    FENCE("_fence"),
    FENCE_GATE("_fence_gate"),
    /** Anything else (doors, panes, plants, ...): never repainted. */
    OTHER(null);

    private final String suffix;

    ShapeKind(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isVariant() {
        return suffix != null && !suffix.isEmpty();
    }

    /** The variant kind encoded in a block id's suffix, if any. */
    public static Optional<ShapeKind> fromSuffix(String id) {
        // FENCE_GATE before FENCE so "_fence_gate" is not read as a fence.
        for (ShapeKind kind : new ShapeKind[]{FENCE_GATE, STAIRS, SLAB, WALL, FENCE}) {
            if (id.endsWith(kind.suffix)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
