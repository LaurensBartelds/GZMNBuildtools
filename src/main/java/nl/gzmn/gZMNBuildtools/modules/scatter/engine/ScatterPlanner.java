package nl.gzmn.gZMNBuildtools.modules.scatter.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BlockSource;
import nl.gzmn.gZMNBuildtools.shared.terrain.TerrainMath;
import nl.gzmn.gZMNBuildtools.shared.terrain.TerrainView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Picks where prefabs go inside a brush circle: spread out like a natural
 * scatter (random points kept apart), each set down on the ground so no part
 * of its base floats, and skipped on steep slopes, roofs, water and spots
 * already taken by trees or builds.
 */
public final class ScatterPlanner {

    /**
     * One prefab to place.
     *
     * @param y    the first block above the ground (before any sinking)
     * @param seed for prefabs that vary each time (procedural trees and rocks)
     */
    public record Placement(int prefab, int x, int y, int z, int turns, boolean flip, long seed) {
    }

    /** What the planner needs to know about blocks. */
    public record Rules(Predicate<String> ground, Predicate<String> solid, Predicate<String> natural,
                        Predicate<String> liquid) {
    }

    private static final int ATTEMPTS_PER_PLACEMENT = 25;
    private static final int MAX_STEP = 3;

    private ScatterPlanner() {
    }

    public static List<Placement> plan(BlockSource world, Rules rules, List<Footprint> prefabs, int cx, int cy,
                                       int cz, double radius, ScatterOptions options, long seed) {
        List<Placement> out = new ArrayList<>();
        if (prefabs.isEmpty()) {
            return out;
        }
        TerrainView ground = new TerrainView() {
            @Override
            public boolean isSolid(int x, int y, int z) {
                String id = world.blockAt(x, y, z);
                return id != null && rules.ground().test(id);
            }

            @Override
            public int minY() {
                return world.minY();
            }

            @Override
            public int maxY() {
                return world.maxY();
            }
        };
        double averageReach = prefabs.stream().mapToDouble(Footprint::reach).average().orElse(1);
        double typicalSpacing = options.spacing() > 0 ? options.spacing() : 1.6 * averageReach + 1;
        int target = Math.max(1, (int) Math.round(options.density() * Math.PI * radius * radius
                / (typicalSpacing * typicalSpacing)));
        int window = Math.max(4, (int) Math.ceil(radius));
        Random random = new Random(seed);
        List<double[]> taken = new ArrayList<>();
        for (int attempt = 0; attempt < target * ATTEMPTS_PER_PLACEMENT && out.size() < target; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radius * Math.sqrt(random.nextDouble());
            int x = cx + (int) Math.round(Math.cos(angle) * distance);
            int z = cz + (int) Math.round(Math.sin(angle) * distance);
            int index = random.nextInt(prefabs.size());
            int turns = options.rotate() ? random.nextInt(4) : 0;
            boolean flip = options.flip() && random.nextBoolean();
            long prefabSeed = random.nextLong();
            Footprint footprint = prefabs.get(index);

            double reach = footprint.reach();
            if (tooClose(taken, x, z, reach, options.spacing())) {
                continue;
            }
            int[] spot = groundUnder(world, ground, rules, footprint.base(turns, flip), x, z, cy, window, options);
            if (spot == null || !roomAbove(world, rules, x, spot[1] + 1, z, footprint.height())) {
                continue;
            }
            out.add(new Placement(index, x, spot[0], z, turns, flip, prefabSeed));
            taken.add(new double[]{x, z, reach});
        }
        return out;
    }

    private static boolean tooClose(List<double[]> taken, int x, int z, double reach, double spacing) {
        for (double[] t : taken) {
            double min = spacing > 0 ? spacing : 0.8 * (reach + t[2]) + 1;
            if (Math.hypot(t[0] - x, t[1] - z) < min) {
                return true;
            }
        }
        return false;
    }

    /**
     * The y the prefab's bottom layer goes at and the ground height at the
     * anchor, or null when the spot is unsuitable.
     */
    private static int[] groundUnder(BlockSource world, TerrainView ground, Rules rules, List<int[]> base, int x,
                                       int z, int cy, int window, ScatterOptions options) {
        int centre = TerrainMath.surfaceNear(ground, x, z, cy, window);
        if (centre == TerrainMath.NO_SURFACE) {
            return null;
        }
        String top = world.blockAt(x, centre, z);
        if (top == null || (!options.anyGround() && !rules.natural().test(top))) {
            return null;
        }
        String above = world.blockAt(x, centre + 1, z);
        if (above != null && rules.liquid().test(above)) {
            return null;
        }
        if (TerrainMath.slopeDegrees(ground, x, centre, z, 2) > options.maxSlope()) {
            return null;
        }
        int lowest = centre;
        int highest = centre;
        for (int[] c : base) {
            int h = TerrainMath.surfaceNear(ground, x + c[0], z + c[1], centre, MAX_STEP + 1);
            if (h == TerrainMath.NO_SURFACE) {
                return null;
            }
            lowest = Math.min(lowest, h);
            highest = Math.max(highest, h);
        }
        if (highest - lowest > MAX_STEP) {
            return null;
        }
        // Sit on the lowest ground under the base so no corner floats.
        return new int[]{lowest + 1, centre};
    }

    /** No tree, build or other prefab already in the anchor column. */
    private static boolean roomAbove(BlockSource world, Rules rules, int x, int y, int z, int height) {
        int top = Math.min(world.maxY(), y + Math.max(2, Math.min(height, 12)));
        for (int yy = y; yy <= top; yy++) {
            String id = world.blockAt(x, yy, z);
            if (id != null && rules.solid().test(id)) {
                return false;
            }
        }
        return true;
    }
}
