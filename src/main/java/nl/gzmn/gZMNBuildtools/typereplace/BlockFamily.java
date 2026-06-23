package nl.gzmn.gZMNBuildtools.typereplace;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A resolved block family: a base material and the concrete {@link BlockType}s
 * for each connecting variant it actually has in the world's block registry.
 *
 * <p>This is a pure value object; the catalogue of which materials exist lives
 * in {@link nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry}.</p>
 */
public class BlockFamily {

    private final String baseMaterial;
    private final Map<String, BlockType> variants = new HashMap<>();

    /**
     * @param baseMaterial the material name (e.g. {@code "stone"})
     * @param definition   how to resolve its variants, or {@code null} to fall
     *                     back to generic suffix-based discovery
     */
    public BlockFamily(String baseMaterial, MaterialDefinition definition) {
        this.baseMaterial = baseMaterial.toLowerCase().replace("minecraft:", "");
        discoverVariants(definition);
    }

    private void discoverVariants(MaterialDefinition def) {
        if (def != null) {
            addVariantIfExists("block", def.blockName);
            if (def.hasStairs)
                addVariantIfExists("stairs", def.variantPrefix + "_stairs");
            if (def.hasSlab)
                addVariantIfExists("slab", def.variantPrefix + "_slab");
            if (def.hasWall)
                addVariantIfExists("wall", def.variantPrefix + "_wall");
            if (def.hasFence)
                addVariantIfExists("fence", def.variantPrefix + "_fence");
            if (def.hasFenceGate)
                addVariantIfExists("fence_gate", def.variantPrefix + "_fence_gate");
            if (def.hasBars)
                addVariantIfExists("bars", def.barsName != null ? def.barsName : def.variantPrefix + "_bars");
            if (def.hasGrate && def.grateName != null)
                addVariantIfExists("grate", def.grateName);
        } else {
            discoverGenericVariants();
        }
    }

    private void discoverGenericVariants() {
        addVariantIfExists("block", baseMaterial);
        addVariantIfExists("block", baseMaterial + "s");
        addVariantIfExists("block", baseMaterial + "_block");
        addVariantIfExists("block", baseMaterial + "_planks");

        addVariantIfExists("stairs", baseMaterial + "_stairs");
        addVariantIfExists("slab", baseMaterial + "_slab");
        addVariantIfExists("wall", baseMaterial + "_wall");
        addVariantIfExists("fence", baseMaterial + "_fence");
        addVariantIfExists("fence_gate", baseMaterial + "_fence_gate");
        addVariantIfExists("bars", baseMaterial + "_bars");
        addVariantIfExists("grate", baseMaterial + "_grate");
    }

    private void addVariantIfExists(String variantType, String blockId) {
        if (variants.containsKey(variantType)) {
            return;
        }
        BlockType block = BlockTypes.get("minecraft:" + blockId);
        if (block != null) {
            variants.put(variantType, block);
        }
    }

    public BlockType getVariant(String variantType) {
        return variants.get(variantType);
    }

    public boolean hasVariant(String variantType) {
        return variants.containsKey(variantType);
    }

    public Map<String, BlockType> getVariants() {
        return Collections.unmodifiableMap(variants);
    }

    public String getBaseMaterial() {
        return baseMaterial;
    }

    @Override
    public String toString() {
        return "BlockFamily{baseMaterial='" + baseMaterial + "', variants=" + variants.keySet() + '}';
    }
}
