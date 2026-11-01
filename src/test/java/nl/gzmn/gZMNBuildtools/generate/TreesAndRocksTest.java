package nl.gzmn.gZMNBuildtools.generate;

import nl.gzmn.gZMNBuildtools.brush.BrushArguments;
import nl.gzmn.gZMNBuildtools.brush.TerrainBrushType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TreesAndRocksTest {

    record Part(int x, int y, int z, int pattern, char axis) {
    }

    private static List<Part> tree(TreeStyle style, long seed) {
        List<Part> parts = new ArrayList<>();
        Trees.generate(style, 0, 64, 0, 12, 1, 3, seed, (x, y, z, p, a) -> parts.add(new Part(x, y, z, p, a)));
        return parts;
    }

    @Test
    void everyStyleHasATrunkFromTheBaseAndLeavesUnlessDead() {
        for (TreeStyle style : TreeStyle.values()) {
            List<Part> parts = tree(style, 7);
            assertTrue(parts.stream().anyMatch(p -> p.x == 0 && p.y == 64 && p.z == 0 && p.pattern == Trees.LOG),
                    style + " has no trunk base");
            long leaves = parts.stream().filter(p -> p.pattern == Trees.LEAVES).count();
            if (style == TreeStyle.DEAD) {
                assertEquals(0, leaves);
            } else {
                assertTrue(leaves > 10, style + " has too few leaves: " + leaves);
            }
            assertTrue(parts.stream().allMatch(p -> p.y >= 60), style + " grows below its base");
        }
    }

    @Test
    void eachBlockIsReportedOnceAndLogsWinOverLeaves() {
        for (TreeStyle style : TreeStyle.values()) {
            Map<String, Integer> seen = new HashMap<>();
            for (Part p : tree(style, 3)) {
                assertNull(seen.put(p.x + "," + p.y + "," + p.z, p.pattern), style + " repeats a block");
            }
        }
    }

    @Test
    void trunkLogsStandUprightAndBranchesLieAlongTheirDirection() {
        List<Part> oak = tree(TreeStyle.OAK, 11);
        assertEquals('y', oak.stream().filter(p -> p.x == 0 && p.y == 64 && p.z == 0).findFirst().orElseThrow().axis);
        assertTrue(oak.stream().anyMatch(p -> p.pattern == Trees.LOG && p.axis != 'y'), "oak branches should be sideways");
    }

    @Test
    void spruceNarrowsTowardsTheTop() {
        List<Part> spruce = tree(TreeStyle.SPRUCE, 5);
        int low = (int) spruce.stream().filter(p -> p.pattern == Trees.LEAVES && p.y == 68).count();
        int high = (int) spruce.stream().filter(p -> p.pattern == Trees.LEAVES && p.y == 75).count();
        assertTrue(low > high, "low=" + low + " high=" + high);
    }

    @Test
    void sameSeedGivesTheSameTree() {
        assertEquals(tree(TreeStyle.WILLOW, 42).size(), tree(TreeStyle.WILLOW, 42).size());
    }

    @Test
    void rocksSinkIntoTheGroundAndFlattenOnRequest() {
        List<int[]> sitting = new ArrayList<>();
        Rocks.generate(0, 63, 0, 4, 0, 0, 0, 1, (x, y, z) -> sitting.add(new int[]{x, y, z}));
        assertEquals(64, sitting.stream().mapToInt(b -> b[1]).min().orElseThrow());

        List<int[]> buried = new ArrayList<>();
        Rocks.generate(0, 63, 0, 4, 0, 0, 0.5, 1, (x, y, z) -> buried.add(new int[]{x, y, z}));
        assertTrue(buried.stream().anyMatch(b -> b[1] <= 63));

        List<int[]> slab = new ArrayList<>();
        Rocks.generate(0, 63, 0, 4, 0.7, 0, 0, 1, (x, y, z) -> slab.add(new int[]{x, y, z}));
        int roundHeight = sitting.stream().mapToInt(b -> b[1]).max().orElseThrow() - 64;
        int slabHeight = slab.stream().mapToInt(b -> b[1]).max().orElseThrow() - 64;
        assertTrue(slabHeight < roundHeight);
    }

    @Test
    void treeArgumentsTakeTwoPatternsAndANamedStyle() {
        BrushArguments<TerrainBrushType> args = BrushArguments.parse(TerrainBrushType.TREE,
                "spruce_log spruce_leaves style=spruce height=14");
        assertEquals(List.of("spruce_log", "spruce_leaves"), args.patterns());
        assertEquals("spruce", args.choice("style"));
        assertEquals(14, args.getInt("height"));
        assertEquals("oak", BrushArguments.parse(TerrainBrushType.TREE, "oak_log oak_leaves").choice("style"));
        assertThrows(IllegalArgumentException.class,
                () -> BrushArguments.parse(TerrainBrushType.TREE, "oak_log oak_leaves style=banana"));
        assertThrows(IllegalArgumentException.class, () -> BrushArguments.parse(TerrainBrushType.TREE, "oak_log"));
        assertEquals("tree", GeneratorType.TREE.id());
        assertEquals(2, GeneratorType.TREE.patternSlots());
    }
}
