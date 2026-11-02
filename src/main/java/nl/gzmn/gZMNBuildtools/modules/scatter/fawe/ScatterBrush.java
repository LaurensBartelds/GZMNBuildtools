package nl.gzmn.gZMNBuildtools.modules.scatter.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.Footprint;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.ScatterOptions;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.ScatterPlanner;
import nl.gzmn.gZMNBuildtools.modules.scatter.prefab.Prefab;
import nl.gzmn.gZMNBuildtools.shared.build.LongHashSet;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.ExtentBlockSource;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * Scatters prefabs inside the brush radius. The layout for the next click is
 * fixed in advance (a seed that moves on after every click), so the hover
 * preview shows exactly what the click will place.
 */
public class ScatterBrush implements PreviewableBrush {

    private final List<Prefab> prefabs;
    private final List<Footprint> footprints;
    private final ScatterOptions options;
    private final ScatterPlanner.Rules rules;
    private final int maxBlocks;
    private final AtomicLong seed;
    private final BiConsumer<Integer, Integer> feedback;

    /** @param feedback receives (prefabs placed, blocks set) after each click */
    public ScatterBrush(List<Prefab> prefabs, ScatterOptions options, ScatterPlanner.Rules rules, int maxBlocks,
                        long seed, BiConsumer<Integer, Integer> feedback) {
        this.prefabs = List.copyOf(prefabs);
        this.footprints = prefabs.stream().map(Prefab::footprint).toList();
        this.options = options;
        this.rules = rules;
        this.maxBlocks = maxBlocks;
        this.seed = new AtomicLong(seed);
        this.feedback = feedback;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        long current = seed.get();
        int[] result = run(editSession, position, size, current, BuildEditor.StateSink.into(editSession));
        seed.compareAndSet(current, new Random(current).nextLong());
        feedback.accept(result[0], result[1]);
    }

    @Override
    public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        run(editSession, position, size, seed.get(), sink);
    }

    @Override
    public Object previewState() {
        return seed.get();
    }

    /** @return {prefabs placed, blocks set} */
    private int[] run(EditSession editSession, BlockVector3 position, double size, long layout,
                      BuildEditor.StateSink sink) {
        ExtentBlockSource source = new ExtentBlockSource(editSession);
        List<ScatterPlanner.Placement> plan = ScatterPlanner.plan(source, rules, footprints, position.x(),
                position.y(), position.z(), size, options, layout);
        LongHashSet used = new LongHashSet(4096);
        int minY = editSession.getMinY();
        int maxY = editSession.getMaxY();
        int[] blocks = {0};
        int placed = 0;
        for (ScatterPlanner.Placement p : plan) {
            if (blocks[0] >= maxBlocks) {
                break;
            }
            Prefab prefab = prefabs.get(p.prefab());
            int buried = options.sink() >= 0 ? options.sink() : prefab.groundLayers();
            prefab.emit(p.x(), p.y() - buried, p.z(), p.turns(), p.flip(), p.seed(), (x, y, z, state) -> {
                if (y < minY || y > maxY || blocks[0] >= maxBlocks || !used.add(LongHashSet.pack(x, y, z))) {
                    return;
                }
                if (!options.replace()) {
                    BlockMaterial existing = editSession.getBlock(x, y, z).getBlockType().getMaterial();
                    if (existing.isSolid() || existing.isLiquid()) {
                        return;
                    }
                }
                BlockState current = editSession.getBlock(x, y, z);
                if (!current.equals(state)) {
                    sink.accept(x, y, z, state);
                    blocks[0]++;
                }
            });
            placed++;
        }
        return new int[]{placed, blocks[0]};
    }
}
