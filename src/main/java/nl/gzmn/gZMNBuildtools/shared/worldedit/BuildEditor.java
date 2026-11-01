package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.shared.build.BlockSource;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScanner;
import nl.gzmn.gZMNBuildtools.shared.build.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.build.TargetPolicy;

import java.util.Collection;
import java.util.Set;

/**
 * Runs a build-aware edit at a clicked block: finds the connected build, lets
 * a tool choose what each covered block becomes, and writes the result with
 * each block's state (facing, half, waterlogged, ...) carried over.
 */
public final class BuildEditor {

    /** Receives a placed block; writes to the edit session or collects a preview. */
    @FunctionalInterface
    public interface StateSink {
        void accept(int x, int y, int z, BlockState state);

        static StateSink into(EditSession editSession) {
            return editSession::setBlock;
        }
    }

    /** Builds the per-block choice once the build is known (e.g. from its size, materials or neighbours). */
    @FunctionalInterface
    public interface Plan {
        BuildPainter.Chooser chooserFor(BuildScan scan, BlockSource source);
    }

    /** A scanned build, or why there is none. */
    public record Scan(BuildScan scan, TargetPolicy policy, ExtentBlockSource source, String problem) {
        public boolean ok() {
            return problem == null;
        }
    }

    /** What an edit did. */
    public record Outcome(int painted, int unchanged, BuildScan scan, String problem) {
        public boolean ok() {
            return problem == null;
        }
    }

    private final BlockKnowledge blocks;
    private final BuildPainter painter;

    public BuildEditor(BlockKnowledge blocks) {
        this.blocks = blocks;
        this.painter = new BuildPainter(blocks.rules());
    }

    public BlockKnowledge blocks() {
        return blocks;
    }

    /**
     * Finds the build connected to {@code click}.
     *
     * @param paintsWith materials the tool places, which count as part of the build on later strokes
     */
    public Scan scan(EditSession editSession, BlockVector3 click, TargetMode mode, Collection<String> paintsWith) {
        ExtentBlockSource source = new ExtentBlockSource(editSession);
        String clicked = source.blockAt(click.x(), click.y(), click.z());
        if (clicked == null) {
            return new Scan(null, null, source, "Aim at a block of the build.");
        }
        TargetPolicy policy = painter.policy(mode, clicked, paintsWith, Set.of());
        BuildScan scan = BuildScanner.scan(source, click.x(), click.y(), click.z(), policy, blocks.rules(),
                blocks.maxScanBlocks(), blocks.maxScanRadius());
        if (scan.size() == 0) {
            return new Scan(scan, policy, source, clicked + " is not part of a build these tools work on (it is a "
                    + "detail or terrain block). Try target=all.");
        }
        return new Scan(scan, policy, source, null);
    }

    /**
     * Edits the build at {@code click}.
     *
     * @param radius brush radius, or null for the whole build
     * @param sink   where placed blocks go
     */
    public Outcome run(EditSession editSession, BlockVector3 click, TargetMode mode, Collection<String> paintsWith,
                       Double radius, Plan plan, StateSink sink) {
        Scan found = scan(editSession, click, mode, paintsWith);
        if (!found.ok()) {
            return new Outcome(0, 0, found.scan(), found.problem());
        }
        BuildPainter.StrokeFilter stroke = radius == null ? BuildPainter.StrokeFilter.ALL
                : BuildPainter.StrokeFilter.sphere(click.x(), click.y(), click.z(), radius);
        BuildPainter.Chooser chooser;
        try {
            chooser = plan.chooserFor(found.scan(), found.source());
        } catch (IllegalArgumentException e) {
            return new Outcome(0, 0, found.scan(), e.getMessage());
        }
        BuildPainter.Result result = painter.paint(found.source(), found.scan(), found.policy(), stroke, chooser,
                (x, y, z, from, to) -> place(editSession, x, y, z, to, sink));
        return new Outcome(result.painted(), result.unchanged(), found.scan(), null);
    }

    /** Places {@code targetId} at a position, keeping the current block's state where it applies. */
    public void place(EditSession editSession, int x, int y, int z, String targetId, StateSink sink) {
        BlockType target = blocks.type(targetId);
        if (target != null) {
            sink.accept(x, y, z, StateMapper.map(editSession.getBlock(x, y, z), target));
        }
    }
}
