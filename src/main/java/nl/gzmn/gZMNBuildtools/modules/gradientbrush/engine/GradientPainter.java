package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Decides, for each block of a scanned build, which block the gradient puts
 * there: the palette material at that height (dithered), in the same shape as
 * the block it replaces.
 */
public final class GradientPainter {

    /** Receives each change; the source id is given so block states can be carried over. */
    @FunctionalInterface
    public interface Writer {
        void write(int x, int y, int z, String sourceId, String targetId);
    }

    /** What happened in one paint pass. */
    public record Result(int painted, int unchanged, int skippedShapes) {
    }

    private final BlockRules rules;
    private final VariantResolver variants;

    public GradientPainter(BlockRules rules, VariantResolver variants) {
        this.rules = rules;
        this.variants = variants;
    }

    /** Policy for a click: the clicked material plus everything the palette paints with. */
    public TargetPolicy policy(TargetMode mode, String clickedId, Palette palette, Set<String> extraGroups) {
        Set<String> groups = new HashSet<>(extraGroups);
        if (clickedId != null) {
            groups.add(rules.group(clickedId));
        }
        if (palette != null) {
            palette.blocks().forEach(b -> groups.add(rules.group(b)));
        }
        return new TargetPolicy(mode, rules, groups);
    }

    /** Seed shared by every stroke on the same build, so dithering lines up. */
    public static long seedFor(Bounds bounds) {
        return bounds.minX() * 73856093L ^ bounds.minY() * 19349663L ^ bounds.minZ() * 83492791L;
    }

    /**
     * Paints the scanned build's blocks for which {@code inStroke} is true
     * (a brush sphere, or everything for a whole-build paint).
     */
    public Result paint(BlockSource source, BuildScan scan, TargetPolicy policy, Palette palette,
                        GradientFrame frame, GradientSettings settings, StrokeFilter inStroke, Writer writer) {
        Set<String> mainGroups = scan.mainGroups();
        long seed = seedFor(scan.bounds());
        int[] counts = new int[3];
        scan.members().forEach(key -> {
            int x = LongHashSet.unpackX(key);
            int y = LongHashSet.unpackY(key);
            int z = LongHashSet.unpackZ(key);
            if (!inStroke.test(x, y, z)) {
                return;
            }
            String id = source.blockAt(x, y, z);
            if (!policy.paint(id, mainGroups)) {
                return;
            }
            BlockCatalog.Info info = rules.catalog().classify(id);
            String base = palette.blockAt(frame.t(x, y, z),
                    settings.dither().threshold(x, y, z, seed), Dither.hash01(x, y, z, ~seed), settings.softness());
            Optional<String> target = settings.keepShapes() || info.kind() == ShapeKind.OTHER
                    ? variants.resolve(base, info.kind())
                    : Optional.of(base);
            if (target.isEmpty()) {
                counts[2]++;
            } else if (target.get().equals(info.id())) {
                counts[1]++;
            } else {
                writer.write(x, y, z, info.id(), target.get());
                counts[0]++;
            }
        });
        return new Result(counts[0], counts[1], counts[2]);
    }

    /** Which positions a paint pass covers. */
    @FunctionalInterface
    public interface StrokeFilter {
        boolean test(int x, int y, int z);

        StrokeFilter ALL = (x, y, z) -> true;

        static StrokeFilter sphere(int cx, int cy, int cz, double radius) {
            double r2 = (radius + 0.5) * (radius + 0.5);
            return (x, y, z) -> {
                double dx = x - cx;
                double dy = y - cy;
                double dz = z - cz;
                return dx * dx + dy * dy + dz * dz <= r2;
            };
        }
    }
}
