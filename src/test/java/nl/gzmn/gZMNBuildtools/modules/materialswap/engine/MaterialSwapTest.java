package nl.gzmn.gZMNBuildtools.modules.materialswap.engine;

import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScanner;
import nl.gzmn.gZMNBuildtools.shared.build.TargetPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static nl.gzmn.gZMNBuildtools.shared.build.BuildFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialSwapTest {

    private final MaterialSwap swap = new MaterialSwap(CATALOG, VARIANTS);
    private final BuildPainter painter = new BuildPainter(RULES);

    private World swapped(String options, int clickX, int clickY, int clickZ) {
        World w = tower();
        w.set(1, 64, 0, "oak_planks");
        SwapOptions o = SwapOptions.parse(options, CATALOG::exists, 32, 4);
        TargetPolicy policy = painter.policy(swap.mode(o), w.get(clickX, clickY, clickZ),
                List.of(swap.targetMaterial(o)), Set.of());
        BuildScan scan = BuildScanner.scan(w, clickX, clickY, clickZ, policy, RULES, 100_000, 64);
        painter.paint(w, scan, policy, BuildPainter.StrokeFilter.ALL, swap.chooser(o), (x, y, z, f, t) -> w.set(x, y, z, t));
        return w;
    }

    @Test
    void swapsTheClickedMaterialKeepingShapesTexturesAndDetails() {
        World w = swapped("cobblestone", 0, 66, 0);
        assertEquals("cobblestone", w.get(0, 66, 0));
        assertEquals("cobblestone_stairs", w.get(1, 75, 0), "stairs stay stairs");
        assertEquals("mossy_cobblestone", w.get(2, 72, 4), "mossy stays mossy");
        assertEquals("glass", w.get(0, 70, 2));
        assertEquals("oak_door", w.get(2, 64, 0));
        assertEquals("oak_planks", w.get(1, 64, 0), "other materials are left alone");
        assertEquals("grass_block", w.get(0, 63, 0));
    }

    @Test
    void materialsWithoutTheTextureFallBackToThePlainBlock() {
        World w = swapped("deepslate_bricks", 0, 66, 0);
        assertEquals("deepslate_bricks", w.get(2, 72, 4), "deepslate has no mossy bricks");
        assertEquals("deepslate_brick_stairs", w.get(1, 75, 0));
        assertEquals("cobblestone", swapped("cobblestone textures=plain", 0, 66, 0).get(2, 72, 4));
    }

    @Test
    void fromLimitsTheSwapToOneMaterialAnywhereInTheBuild() {
        // Clicking the oak trim with from=stone_bricks still swaps the stone bricks, not the oak.
        World w = swapped("andesite from=stone_bricks", 1, 64, 0);
        assertEquals("oak_planks", w.get(1, 64, 0));
        assertEquals("andesite", w.get(0, 66, 0));
        assertEquals("andesite_stairs", w.get(1, 75, 0));
    }

    @Test
    void fullShapesAndTargetVariantsAreUnderstood() {
        assertEquals("cobblestone", swapped("cobblestone shapes=full", 0, 66, 0).get(1, 75, 0));
        // Naming a stairs block means its material.
        assertEquals("cobblestone_stairs", swapped("cobblestone_stairs", 0, 66, 0).get(1, 75, 0));
    }

    @Test
    void optionsRejectNonsense() {
        assertThrows(IllegalArgumentException.class, () -> SwapOptions.parse("", CATALOG::exists, 32, 4));
        assertThrows(IllegalArgumentException.class, () -> SwapOptions.parse("notablock", CATALOG::exists, 32, 4));
        assertThrows(IllegalArgumentException.class, () -> SwapOptions.parse("stone radius=99", CATALOG::exists, 32, 4));
        assertThrows(IllegalArgumentException.class, () -> SwapOptions.parse("stone shapes=maybe", CATALOG::exists, 32, 4));
        assertThrows(IllegalArgumentException.class, () -> SwapOptions.parse("stone andesite", CATALOG::exists, 32, 4));
        assertEquals(6, SwapOptions.parse("stone radius=6", CATALOG::exists, 32, 4).radius());
    }
}
