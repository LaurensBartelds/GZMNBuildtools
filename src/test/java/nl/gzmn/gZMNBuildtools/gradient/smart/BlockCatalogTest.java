package nl.gzmn.gZMNBuildtools.gradient.smart;

import org.junit.jupiter.api.Test;

import static nl.gzmn.gZMNBuildtools.gradient.smart.SmartFixtures.CATALOG;
import static nl.gzmn.gZMNBuildtools.gradient.smart.SmartFixtures.VARIANTS;
import static org.junit.jupiter.api.Assertions.*;

class BlockCatalogTest {

    @Test
    void derivesVariantBasesFromTheBlockList() {
        assertEquals("stone_bricks", CATALOG.classify("minecraft:stone_brick_stairs").base());
        assertEquals(ShapeKind.STAIRS, CATALOG.classify("stone_brick_stairs").kind());
        assertEquals("oak_planks", CATALOG.classify("oak_fence").base());
        assertEquals(ShapeKind.FENCE_GATE, CATALOG.classify("oak_fence_gate").kind());
        assertEquals("quartz_block", CATALOG.classify("quartz_stairs").base());
        assertEquals("deepslate_tiles", CATALOG.classify("deepslate_tile_wall").base());
        assertEquals("nether_bricks", CATALOG.classify("nether_brick_fence").base());
        assertEquals("smooth_stone", CATALOG.classify("smooth_stone_slab").base());
    }

    @Test
    void groupsTextureVariantsWithTheirPlainBlock() {
        assertEquals("stone_bricks", CATALOG.classify("mossy_stone_bricks").group());
        assertEquals("stone_bricks", CATALOG.classify("cracked_stone_bricks").group());
        assertEquals("stone_bricks", CATALOG.classify("chiseled_stone_bricks").group());
        assertEquals("stone_bricks", CATALOG.classify("mossy_stone_brick_stairs").group());
        assertEquals("cobblestone", CATALOG.classify("mossy_cobblestone").group());
    }

    @Test
    void nonBuildingBlocksAreOther() {
        assertEquals(ShapeKind.OTHER, CATALOG.classify("oak_door").kind());
        assertEquals(ShapeKind.OTHER, CATALOG.classify("torch").kind());
        assertEquals(ShapeKind.FULL, CATALOG.classify("stone_bricks").kind());
    }

    @Test
    void resolvesOwnVariantsAndFallsBackToClosestColour() {
        assertEquals("deepslate_brick_stairs", VARIANTS.resolve("deepslate_bricks", ShapeKind.STAIRS).orElseThrow());
        assertEquals("oak_slab", VARIANTS.resolve("oak_planks", ShapeKind.SLAB).orElseThrow());
        assertEquals("andesite", VARIANTS.resolve("andesite", ShapeKind.FULL).orElseThrow());
        // White concrete has no stairs: take the closest-coloured material that does.
        String white = VARIANTS.resolve("white_concrete", ShapeKind.STAIRS).orElseThrow();
        assertTrue(white.endsWith("_stairs"), white);
        assertTrue(VARIANTS.resolve("stone_bricks", ShapeKind.OTHER).isEmpty());
    }
}
