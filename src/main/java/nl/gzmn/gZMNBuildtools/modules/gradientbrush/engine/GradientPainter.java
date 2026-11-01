package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BlockRules;
import nl.gzmn.gZMNBuildtools.shared.build.BlockSource;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.build.TargetPolicy;
import nl.gzmn.gZMNBuildtools.shared.build.VariantResolver;

import java.util.List;
import java.util.Set;

/**
 * The gradient's choice for each block of a build: the palette material at its
 * place along the gradient (dithered), in the same shape as the block it replaces.
 */
public final class GradientPainter {

    private final BuildPainter painter;
    private final VariantResolver variants;

    public GradientPainter(BlockRules rules, VariantResolver variants) {
        this.painter = new BuildPainter(rules);
        this.variants = variants;
    }

    /** Policy for a click: the clicked material plus everything the palette paints with. */
    public TargetPolicy policy(TargetMode mode, String clickedId, Palette palette, Set<String> extraGroups) {
        return painter.policy(mode, clickedId, palette == null ? List.of() : palette.blocks(), extraGroups);
    }

    /** Seed shared by every stroke on the same build, so dithering lines up. */
    public static long seedFor(Bounds bounds) {
        return bounds.minX() * 73856093L ^ bounds.minY() * 19349663L ^ bounds.minZ() * 83492791L;
    }

    /** The gradient as a {@link BuildPainter.Chooser}. */
    public BuildPainter.Chooser chooser(Palette palette, GradientFrame frame, GradientSettings settings, long seed) {
        return (x, y, z, info) -> {
            String base = palette.blockAt(frame.t(x, y, z), settings.dither().threshold(x, y, z, seed),
                    Dither.hash01(x, y, z, ~seed), settings.softness());
            if (settings.keepShapes() || info.kind() == ShapeKind.OTHER) {
                return variants.resolve(base, info.kind()).orElse(null);
            }
            return base;
        };
    }

    public BuildPainter.Result paint(BlockSource source, BuildScan scan, TargetPolicy policy, Palette palette,
                                     GradientFrame frame, GradientSettings settings,
                                     BuildPainter.StrokeFilter stroke, BuildPainter.Writer writer) {
        return painter.paint(source, scan, policy, stroke,
                chooser(palette, frame, settings, seedFor(scan.bounds())), writer);
    }
}
