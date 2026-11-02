package nl.gzmn.gZMNBuildtools.modules.materialswap.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BlockCatalog;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.build.VariantResolver;

/**
 * Swaps a build's material for another while keeping what makes it look built:
 * stairs stay stairs (in the new material, or the closest one that has stairs),
 * and mossy/cracked/chiseled blocks become the new material's mossy/cracked/
 * chiseled form when it has one.
 */
public final class MaterialSwap {

    private final BlockCatalog catalog;
    private final VariantResolver variants;

    public MaterialSwap(BlockCatalog catalog, VariantResolver variants) {
        this.catalog = catalog;
        this.variants = variants;
    }

    /** The plain material to swap to (stone_brick_stairs means stone_bricks). */
    public String targetMaterial(SwapOptions options) {
        BlockCatalog.Info info = catalog.classify(options.to());
        return info.base() != null ? info.base() : info.id();
    }

    /** Without {@code from} only the clicked material is swapped; with it, matching blocks anywhere in the build. */
    public TargetMode mode(SwapOptions options) {
        return options.from() == null ? TargetMode.CLICKED : TargetMode.STRUCTURAL;
    }

    /** The block that replaces {@code source}, or null to leave it. */
    public String target(BlockCatalog.Info source, SwapOptions options) {
        if (options.from() != null && !catalog.classify(options.from()).group().equals(source.group())) {
            return null;
        }
        String to = targetMaterial(options);
        String prefix = options.keepTextures() && source.base() != null ? catalog.textureOf(source.base()) : "";
        String base = catalog.withTexture(to, prefix).orElse(to);
        if (source.kind() == ShapeKind.OTHER) {
            return null;
        }
        if (source.kind() == ShapeKind.FULL || !options.keepShapes()) {
            return base;
        }
        // The textured form's own variant (mossy_cobblestone_stairs), else the new material's, else the
        // closest-coloured material that has this shape.
        return catalog.variant(base, source.kind())
                .or(() -> variants.resolve(to, source.kind()))
                .orElse(null);
    }

    public BuildPainter.Chooser chooser(SwapOptions options) {
        return (x, y, z, source) -> target(source, options);
    }
}
