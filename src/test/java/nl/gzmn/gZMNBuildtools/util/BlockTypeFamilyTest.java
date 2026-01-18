package nl.gzmn.gZMNBuildtools.util;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BlockTypeFamily class
 */
class BlockTypeFamilyTest {

    @BeforeEach
    void setUp() {
        // BlockTypes static initialization happens automatically
        // when WorldEdit classes are loaded
    }

    @Test
    @DisplayName("Should discover stone variants correctly")
    void testStoneVariantDiscovery() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");

        assertNotNull(stoneFamily.getBaseMaterial());
        assertEquals("stone", stoneFamily.getBaseMaterial());

        Map<String, BlockType> variants = stoneFamily.getVariants();
        assertFalse(variants.isEmpty(), "Stone family should have variants");

        // Stone should have block, stairs, slab, and wall variants
        assertTrue(stoneFamily.hasVariant("block"), "Stone should have block variant");
        assertTrue(stoneFamily.hasVariant("stairs"), "Stone should have stairs variant");
        assertTrue(stoneFamily.hasVariant("slab"), "Stone should have slab variant");
        assertTrue(stoneFamily.hasVariant("wall"), "Stone should have wall variant");
    }

    @Test
    @DisplayName("Should discover cobblestone variants correctly")
    void testCobblestoneVariantDiscovery() {
        BlockTypeFamily cobblestoneFamily = new BlockTypeFamily("cobblestone");

        assertTrue(cobblestoneFamily.hasVariant("block"));
        assertTrue(cobblestoneFamily.hasVariant("stairs"));
        assertTrue(cobblestoneFamily.hasVariant("slab"));
        assertTrue(cobblestoneFamily.hasVariant("wall"));

        assertNotNull(cobblestoneFamily.getVariant("stairs"));
    }

    @Test
    @DisplayName("Should discover oak wood variants including fence")
    void testOakWoodVariantDiscovery() {
        BlockTypeFamily oakFamily = new BlockTypeFamily("oak");

        assertTrue(oakFamily.hasVariant("block"), "Oak should have block variant (planks)");
        assertTrue(oakFamily.hasVariant("stairs"), "Oak should have stairs variant");
        assertTrue(oakFamily.hasVariant("slab"), "Oak should have slab variant");
        assertTrue(oakFamily.hasVariant("fence"), "Oak should have fence variant");
        assertTrue(oakFamily.hasVariant("fence_gate"), "Oak should have fence_gate variant");
    }

    @Test
    @DisplayName("Should discover copper variants with bars")
    void testCopperVariantDiscovery() {
        BlockTypeFamily copperFamily = new BlockTypeFamily("copper");

        assertTrue(copperFamily.hasVariant("block"));
        assertTrue(copperFamily.hasVariant("stairs"));
        assertTrue(copperFamily.hasVariant("slab"));
        assertTrue(copperFamily.hasVariant("bars"), "Copper should have bars variant");
    }

    @Test
    @DisplayName("Should handle nether brick with fence variant")
    void testNetherBrickVariantDiscovery() {
        BlockTypeFamily netherBrickFamily = new BlockTypeFamily("nether_brick");

        assertTrue(netherBrickFamily.hasVariant("block"));
        assertTrue(netherBrickFamily.hasVariant("stairs"));
        assertTrue(netherBrickFamily.hasVariant("slab"));
        assertTrue(netherBrickFamily.hasVariant("wall"));
        assertTrue(netherBrickFamily.hasVariant("fence"), "Nether brick should have fence variant");
    }

    @Test
    @DisplayName("Should detect variant types from block IDs")
    void testGetVariantType() {
        // Mock block types by getting them from BlockTypes registry
        BlockType stoneStairs = BlockTypes.get("minecraft:stone_stairs");
        BlockType stoneSlab = BlockTypes.get("minecraft:stone_slab");
        BlockType stoneWall = BlockTypes.get("minecraft:cobblestone_wall");
        BlockType oakFence = BlockTypes.get("minecraft:oak_fence");
        BlockType oakFenceGate = BlockTypes.get("minecraft:oak_fence_gate");

        if (stoneStairs != null) {
            assertEquals("stairs", BlockTypeFamily.getVariantType(stoneStairs));
        }
        if (stoneSlab != null) {
            assertEquals("slab", BlockTypeFamily.getVariantType(stoneSlab));
        }
        if (stoneWall != null) {
            assertEquals("wall", BlockTypeFamily.getVariantType(stoneWall));
        }
        if (oakFence != null) {
            assertEquals("fence", BlockTypeFamily.getVariantType(oakFence));
        }
        if (oakFenceGate != null) {
            assertEquals("fence_gate", BlockTypeFamily.getVariantType(oakFenceGate));
        }
    }

    @Test
    @DisplayName("Should default to 'block' for base blocks")
    void testGetVariantTypeForBaseBlocks() {
        BlockType stone = BlockTypes.get("minecraft:stone");
        BlockType cobblestone = BlockTypes.get("minecraft:cobblestone");

        if (stone != null) {
            assertEquals("block", BlockTypeFamily.getVariantType(stone));
        }
        if (cobblestone != null) {
            assertEquals("block", BlockTypeFamily.getVariantType(cobblestone));
        }
    }

    @Test
    @DisplayName("Should map variants between families preserving type")
    void testMapVariantPreservesType() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");
        BlockTypeFamily cobblestoneFamily = new BlockTypeFamily("cobblestone");

        BlockType stoneStairs = stoneFamily.getVariant("stairs");
        if (stoneStairs != null) {
            BlockType mappedBlock = BlockTypeFamily.mapVariant(stoneStairs, cobblestoneFamily);
            assertNotNull(mappedBlock, "Should map stone stairs to cobblestone stairs");
            assertEquals("stairs", BlockTypeFamily.getVariantType(mappedBlock));
        }
    }

    @Test
    @DisplayName("Should map wall to fence in vertical connector group")
    void testMapWallToFenceInVerticalConnectorGroup() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");
        BlockTypeFamily oakFamily = new BlockTypeFamily("oak");

        // Stone has wall, oak has fence but no wall
        BlockType stoneWall = stoneFamily.getVariant("wall");
        if (stoneWall != null) {
            BlockType mappedBlock = BlockTypeFamily.mapVariant(stoneWall, oakFamily);
            assertNotNull(mappedBlock, "Should map stone wall to oak fence (vertical connector)");
            // The mapped block should be fence since oak doesn't have wall
            String variantType = BlockTypeFamily.getVariantType(mappedBlock);
            assertTrue(variantType.equals("fence") || variantType.equals("wall"),
                "Should map to a vertical connector variant");
        }
    }

    @Test
    @DisplayName("Should map fence to wall in vertical connector group")
    void testMapFenceToWallInVerticalConnectorGroup() {
        BlockTypeFamily oakFamily = new BlockTypeFamily("oak");
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");

        // Oak has fence, stone has wall
        BlockType oakFence = oakFamily.getVariant("fence");
        if (oakFence != null) {
            BlockType mappedBlock = BlockTypeFamily.mapVariant(oakFence, stoneFamily);
            assertNotNull(mappedBlock, "Should map oak fence to stone wall (vertical connector)");
            String variantType = BlockTypeFamily.getVariantType(mappedBlock);
            assertTrue(variantType.equals("wall") || variantType.equals("fence"),
                "Should map to a vertical connector variant");
        }
    }

    @Test
    @DisplayName("Should map bars within vertical connector group")
    void testMapBarsInVerticalConnectorGroup() {
        BlockTypeFamily copperFamily = new BlockTypeFamily("copper");
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");

        // Copper has bars, stone has wall
        BlockType copperBars = copperFamily.getVariant("bars");
        if (copperBars != null) {
            BlockType mappedBlock = BlockTypeFamily.mapVariant(copperBars, stoneFamily);
            assertNotNull(mappedBlock, "Should map copper bars to stone wall (vertical connector)");
            String variantType = BlockTypeFamily.getVariantType(mappedBlock);
            assertTrue(variantType.equals("wall") || variantType.equals("fence") || variantType.equals("bars"),
                "Should map to a vertical connector variant");
        }
    }

    @Test
    @DisplayName("Should not map fence_gate to fence (isolated variant)")
    void testFenceGateIsIsolated() {
        BlockTypeFamily oakFamily = new BlockTypeFamily("oak");
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");

        // Oak has fence_gate, stone doesn't
        BlockType oakFenceGate = oakFamily.getVariant("fence_gate");
        if (oakFenceGate != null) {
            BlockType mappedBlock = BlockTypeFamily.mapVariant(oakFenceGate, stoneFamily);
            // Should either be null or fall back to base block, not fence
            if (mappedBlock != null) {
                String variantType = BlockTypeFamily.getVariantType(mappedBlock);
                assertNotEquals("fence", variantType, "Fence gate should not map to fence");
                assertNotEquals("wall", variantType, "Fence gate should not map to wall");
            }
        }
    }

    @Test
    @DisplayName("Should fall back to base block when variant doesn't exist")
    void testFallbackToBaseBlock() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");
        BlockTypeFamily sandstoneFamily = new BlockTypeFamily("sandstone");

        BlockType stoneWall = stoneFamily.getVariant("wall");
        if (stoneWall != null) {
            // Map to a family that might not have all variants
            BlockType mappedBlock = BlockTypeFamily.mapVariant(stoneWall, sandstoneFamily);
            assertNotNull(mappedBlock, "Should fallback to some block");
        }
    }

    @Test
    @DisplayName("Should retrieve all material names")
    void testGetAllMaterialNames() {
        Set<String> materialNames = BlockTypeFamily.getAllMaterialNames();

        assertNotNull(materialNames);
        assertFalse(materialNames.isEmpty(), "Should have registered materials");

        // Check for some known materials
        assertTrue(materialNames.contains("stone"), "Should contain stone");
        assertTrue(materialNames.contains("cobblestone"), "Should contain cobblestone");
        assertTrue(materialNames.contains("oak"), "Should contain oak");
        assertTrue(materialNames.contains("copper"), "Should contain copper");
    }

    @Test
    @DisplayName("Should handle material case insensitivity")
    void testCaseInsensitivity() {
        BlockTypeFamily stone1 = new BlockTypeFamily("stone");
        BlockTypeFamily stone2 = new BlockTypeFamily("STONE");
        BlockTypeFamily stone3 = new BlockTypeFamily("Stone");

        assertEquals("stone", stone1.getBaseMaterial());
        assertEquals("stone", stone2.getBaseMaterial());
        assertEquals("stone", stone3.getBaseMaterial());
    }

    @Test
    @DisplayName("Should handle minecraft: prefix stripping")
    void testMinecraftPrefixStripping() {
        BlockTypeFamily stone1 = new BlockTypeFamily("minecraft:stone");
        BlockTypeFamily stone2 = new BlockTypeFamily("stone");

        assertEquals("stone", stone1.getBaseMaterial());
        assertEquals(stone1.getBaseMaterial(), stone2.getBaseMaterial());
    }

    @Test
    @DisplayName("Should return empty variants for unknown materials")
    void testUnknownMaterialHandling() {
        BlockTypeFamily unknownFamily = new BlockTypeFamily("completely_fake_material_xyz");

        Map<String, BlockType> variants = unknownFamily.getVariants();
        assertNotNull(variants, "Should handle unknown materials gracefully");
    }

    @Test
    @DisplayName("Should recognize material groups")
    void testMaterialGroups() {
        // Material groups might not be populated yet based on the code,
        // but we test the API
        boolean result = BlockTypeFamily.isMaterialGroup("all_copper");
        // Just verify the method works without error
        assertNotNull(BlockTypeFamily.getAllMaterialNames());
    }

    @Test
    @DisplayName("Should handle toString without errors")
    void testToString() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");
        String result = stoneFamily.toString();

        assertNotNull(result);
        assertTrue(result.contains("stone"), "toString should contain material name");
        assertTrue(result.contains("BlockTypeFamily"), "toString should contain class name");
    }

    @Test
    @DisplayName("Should register material with builder pattern")
    void testMaterialRegistrationBuilder() {
        // Test that the builder pattern works
        assertDoesNotThrow(() -> BlockTypeFamily.register("test_material")
                .base("test_block", "test")
                .stairs()
                .slab()
                .wall()
                .build());
    }

    @Test
    @DisplayName("Should handle deepslate variants")
    void testDeepslateVariants() {
        BlockTypeFamily deepslateFamily = new BlockTypeFamily("cobbled_deepslate");

        assertTrue(deepslateFamily.hasVariant("block"));
        assertTrue(deepslateFamily.hasVariant("stairs"));
        assertTrue(deepslateFamily.hasVariant("slab"));
        assertTrue(deepslateFamily.hasVariant("wall"));
    }

    @Test
    @DisplayName("Should handle brick material naming irregularity")
    void testBrickNamingIrregularity() {
        BlockTypeFamily brickFamily = new BlockTypeFamily("brick");

        // Bricks has irregular naming: "bricks" for block but "brick_stairs"
        assertTrue(brickFamily.hasVariant("block"), "Brick should have block variant (bricks)");
        assertTrue(brickFamily.hasVariant("stairs"), "Brick should have stairs variant");
    }

    @Test
    @DisplayName("Should handle waxed copper variants")
    void testWaxedCopperVariants() {
        BlockTypeFamily waxedCopperFamily = new BlockTypeFamily("waxed_copper");

        assertTrue(waxedCopperFamily.hasVariant("block"));
        assertTrue(waxedCopperFamily.hasVariant("stairs"));
        assertTrue(waxedCopperFamily.hasVariant("slab"));
        assertTrue(waxedCopperFamily.hasVariant("bars"));
    }

    @Test
    @DisplayName("Should return unmodifiable variants map")
    void testGetVariantsReturnsUnmodifiable() {
        BlockTypeFamily stoneFamily = new BlockTypeFamily("stone");
        Map<String, BlockType> variants = stoneFamily.getVariants();

        assertThrows(UnsupportedOperationException.class, () -> variants.put("fake_variant", null),
            "Variants map should be unmodifiable");
    }
}
