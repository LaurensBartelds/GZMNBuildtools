package nl.gzmn.gZMNBuildtools.typereplace;

import nl.gzmn.gZMNBuildtools.typereplace.registry.InMemoryBlockFamilyRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the migration of the block-family catalogue from in-code definitions to
 * block-families.yml: verifies the bundled resource parses into the expected
 * definitions (and that the historical cut_red_red_sandstone typo is fixed).
 */
class BlockFamilyRegistryTest {

    private final InMemoryBlockFamilyRegistry registry = InMemoryBlockFamilyRegistry.bundled();

    @Test
    void loadsAllBundledFamilies() {
        assertEquals(67, registry.materialNames().size(), "expected 67 block families in block-families.yml");
    }

    @Test
    void cutRedSandstone_typoIsFixed() {
        MaterialDefinition def = registry.definition("cut_red_sandstone");
        assertNotNull(def);
        // Previously this base block was the non-existent "cut_red_red_sandstone".
        assertEquals("cut_red_sandstone", def.blockName);
        assertTrue(def.hasSlab);
        assertFalse(def.hasStairs);
        assertFalse(def.hasWall);
    }

    @Test
    void stone_hasStairsSlabWall() {
        MaterialDefinition def = registry.definition("stone");
        assertNotNull(def);
        assertEquals("stone", def.blockName);
        assertEquals("stone", def.variantPrefix);
        assertTrue(def.hasStairs && def.hasSlab && def.hasWall);
        assertFalse(def.hasFence);
    }

    @Test
    void stoneBrick_usesPluralBlockId() {
        MaterialDefinition def = registry.definition("stone_brick");
        assertNotNull(def);
        assertEquals("stone_bricks", def.blockName);
        assertEquals("stone_brick", def.variantPrefix);
    }

    @Test
    void oak_hasFenceAndFenceGate() {
        MaterialDefinition def = registry.definition("oak");
        assertNotNull(def);
        assertEquals("oak_planks", def.blockName);
        assertEquals("oak", def.variantPrefix);
        assertTrue(def.hasStairs && def.hasSlab && def.hasFence && def.hasFenceGate);
        assertFalse(def.hasWall);
    }

    @Test
    void copper_hasNamedBarsAndGrate() {
        MaterialDefinition def = registry.definition("copper");
        assertNotNull(def);
        assertEquals("copper_block", def.blockName);
        assertEquals("cut_copper", def.variantPrefix);
        assertTrue(def.hasBars);
        assertEquals("copper_bars", def.barsName);
        assertTrue(def.hasGrate);
        assertEquals("copper_grate", def.grateName);
    }

    @Test
    void iron_hasUnnamedBarsOnly() {
        MaterialDefinition def = registry.definition("iron");
        assertNotNull(def);
        assertEquals("iron_block", def.blockName);
        assertTrue(def.hasBars);
        assertNull(def.barsName);
        assertFalse(def.hasGrate);
        assertFalse(def.hasStairs);
        assertFalse(def.hasSlab);
    }

    @Test
    void deepslate_hasNoVariants() {
        MaterialDefinition def = registry.definition("deepslate");
        assertNotNull(def);
        assertFalse(def.hasStairs || def.hasSlab || def.hasWall || def.hasFence || def.hasBars);
    }
}
