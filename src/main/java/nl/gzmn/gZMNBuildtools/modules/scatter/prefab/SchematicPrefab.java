package nl.gzmn.gZMNBuildtools.modules.scatter.prefab;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.Footprint;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.ExtentBlockSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** A prefab loaded from a WorldEdit schematic; air and structure voids are left out. */
public final class SchematicPrefab implements Prefab {

    private record Part(int x, int y, int z, BlockState state) {
    }

    private final String name;
    private final List<Part> parts;
    private final Footprint footprint;
    private final int groundLayers;

    private SchematicPrefab(String name, List<Part> parts, Footprint footprint, int groundLayers) {
        this.name = name;
        this.parts = parts;
        this.footprint = footprint;
        this.groundLayers = groundLayers;
    }

    /**
     * @param terrain   whether a block id is natural ground (to spot a ground layer under the prefab)
     * @param maxBlocks largest prefab accepted
     * @throws IllegalArgumentException when the schematic is empty or too big
     */
    public static SchematicPrefab of(String name, Clipboard clipboard, Predicate<String> terrain, int maxBlocks) {
        List<int[]> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (BlockVector3 pt : clipboard.getRegion()) {
            BlockState state = clipboard.getBlock(pt);
            if (state.getBlockType().getMaterial().isAir() || state.getBlockType() == BlockTypes.STRUCTURE_VOID) {
                continue;
            }
            if (states.size() >= maxBlocks) {
                throw new IllegalArgumentException("'" + name + "' has more than " + maxBlocks
                        + " blocks (scatter.max-prefab-blocks)");
            }
            positions.add(new int[]{pt.x(), pt.y(), pt.z()});
            states.add(state);
        }
        if (states.isEmpty()) {
            throw new IllegalArgumentException("'" + name + "' is empty");
        }
        int minY = positions.stream().mapToInt(p -> p[1]).min().orElseThrow();
        int maxY = positions.stream().mapToInt(p -> p[1]).max().orElseThrow();

        // Bottom layers made only of natural ground are the prefab's own soil.
        int ground = 0;
        while (ground < 2 && minY + ground < maxY && layerIsGround(positions, states, minY + ground, terrain)) {
            ground++;
        }
        // Anchor on the middle of the bottom layer.
        long sumX = 0;
        long sumZ = 0;
        int count = 0;
        for (int[] p : positions) {
            if (p[1] == minY) {
                sumX += p[0];
                sumZ += p[2];
                count++;
            }
        }
        int ax = (int) Math.round((double) sumX / count);
        int az = (int) Math.round((double) sumZ / count);

        List<Part> parts = new ArrayList<>(states.size());
        Set<String> baseColumns = new LinkedHashSet<>();
        List<int[]> base = new ArrayList<>();
        double reach = 0;
        for (int i = 0; i < positions.size(); i++) {
            int[] p = positions.get(i);
            int x = p[0] - ax;
            int y = p[1] - minY;
            int z = p[2] - az;
            parts.add(new Part(x, y, z, states.get(i)));
            reach = Math.max(reach, Math.hypot(x, z));
            if (y == 0 && baseColumns.add(x + "," + z)) {
                base.add(new int[]{x, z});
            }
        }
        return new SchematicPrefab(name, List.copyOf(parts), new Footprint(List.copyOf(base), reach,
                maxY - minY + 1 - ground), ground);
    }

    private static boolean layerIsGround(List<int[]> positions, List<BlockState> states, int y,
                                         Predicate<String> terrain) {
        boolean any = false;
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i)[1] == y) {
                any = true;
                String id = ExtentBlockSource.idOf(states.get(i));
                if (id == null || !terrain.test(id)) {
                    return false;
                }
            }
        }
        return any;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Footprint footprint() {
        return footprint;
    }

    @Override
    public int groundLayers() {
        return groundLayers;
    }

    public int size() {
        return parts.size();
    }

    @Override
    public void emit(int x, int y, int z, int turns, boolean flip, long seed, BuildEditor.StateSink sink) {
        // Mirror first, then turn: the same order as Footprint.base(turns, flip).
        AffineTransform transform = new AffineTransform().rotateY(-90.0 * Math.floorMod(turns, 4))
                .scale(flip ? -1 : 1, 1, 1);
        boolean identity = Math.floorMod(turns, 4) == 0 && !flip;
        for (Part part : parts) {
            Vector3 at = identity ? Vector3.at(part.x(), part.y(), part.z())
                    : transform.apply(Vector3.at(part.x(), part.y(), part.z()));
            BlockState state = identity ? part.state() : BlockTransformExtent.transform(part.state(), transform);
            sink.accept(x + (int) Math.round(at.x()), y + (int) Math.round(at.y()), z + (int) Math.round(at.z()),
                    state);
        }
    }
}
