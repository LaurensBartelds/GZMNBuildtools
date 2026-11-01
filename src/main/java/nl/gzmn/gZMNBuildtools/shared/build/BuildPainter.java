package nl.gzmn.gZMNBuildtools.shared.build;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Walks the blocks of a scanned build that a stroke covers and the target
 * policy allows, and asks a {@link Chooser} what each should become. Shared by
 * every build-aware tool (gradients, material swap, weathering).
 */
public final class BuildPainter {

    /** Receives each change; the source id is given so block states can be carried over. */
    @FunctionalInterface
    public interface Writer {
        void write(int x, int y, int z, String sourceId, String targetId);
    }

    /** Picks the block id to put at a position, or null to leave the block alone. */
    @FunctionalInterface
    public interface Chooser {
        String choose(int x, int y, int z, BlockCatalog.Info source);
    }

    /** What happened in one pass. */
    public record Result(int painted, int unchanged, int skipped) {
    }

    /** Which positions a pass covers. */
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

    private final BlockRules rules;

    public BuildPainter(BlockRules rules) {
        this.rules = rules;
    }

    public BlockRules rules() {
        return rules;
    }

    /**
     * Policy for a click: the clicked material, the materials the tool paints
     * with (so a second stroke over painted blocks still finds the build), and
     * any groups remembered from earlier strokes.
     */
    public TargetPolicy policy(TargetMode mode, String clickedId, Collection<String> paintsWith, Set<String> extraGroups) {
        Set<String> groups = new HashSet<>(extraGroups);
        if (clickedId != null) {
            groups.add(rules.group(clickedId));
        }
        paintsWith.forEach(b -> groups.add(rules.group(b)));
        return new TargetPolicy(mode, rules, groups);
    }

    public Result paint(BlockSource source, BuildScan scan, TargetPolicy policy, StrokeFilter stroke, Chooser chooser,
                        Writer writer) {
        Set<String> mainGroups = scan.mainGroups();
        int[] counts = new int[3];
        scan.members().forEach(key -> {
            int x = LongHashSet.unpackX(key);
            int y = LongHashSet.unpackY(key);
            int z = LongHashSet.unpackZ(key);
            if (!stroke.test(x, y, z)) {
                return;
            }
            String id = source.blockAt(x, y, z);
            if (!policy.paint(id, mainGroups)) {
                return;
            }
            BlockCatalog.Info info = rules.catalog().classify(id);
            String target = chooser.choose(x, y, z, info);
            if (target == null) {
                counts[2]++;
            } else if (target.equals(info.id())) {
                counts[1]++;
            } else {
                writer.write(x, y, z, info.id(), target);
                counts[0]++;
            }
        });
        return new Result(counts[0], counts[1], counts[2]);
    }
}
