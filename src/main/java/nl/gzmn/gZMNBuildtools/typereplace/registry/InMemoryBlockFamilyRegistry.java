package nl.gzmn.gZMNBuildtools.typereplace.registry;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.typereplace.BlockFamily;
import nl.gzmn.gZMNBuildtools.typereplace.MaterialDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-memory {@link BlockFamilyRegistry}. Holds the material definitions and
 * groups; the variant-mapping logic is pure and lives here. Base class for
 * {@link YamlBlockFamilyRegistry}.
 */
public class InMemoryBlockFamilyRegistry implements BlockFamilyRegistry {

    private static final String RESOURCE_NAME = "block-families.yml";

    protected final Map<String, MaterialDefinition> definitions = new LinkedHashMap<>();
    protected final Map<String, List<String>> groups = new LinkedHashMap<>();
    protected final Map<String, List<String>> categories = new LinkedHashMap<>();

    /**
     * Build a registry from the bundled {@code block-families.yml} on the
     * classpath. Used as a default (including in tests) when no plugin-backed
     * registry is injected.
     */
    public static InMemoryBlockFamilyRegistry bundled() {
        InMemoryBlockFamilyRegistry registry = new InMemoryBlockFamilyRegistry();
        try (InputStream in = InMemoryBlockFamilyRegistry.class.getResourceAsStream("/" + RESOURCE_NAME)) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    registry.loadFrom(YamlConfiguration.loadConfiguration(reader));
                }
            }
        } catch (IOException ignored) {
            // Leave the registry empty if the bundled resource cannot be read.
        }
        return registry;
    }

    @Override
    public Set<String> materialNames() {
        Set<String> names = new HashSet<>(definitions.keySet());
        names.addAll(groups.keySet());
        return names;
    }

    @Override
    public BlockFamily family(String name) {
        String key = name.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        return new BlockFamily(key, definitions.get(key));
    }

    /** Direct access to a raw definition (for tests / introspection). */
    public MaterialDefinition definition(String key) {
        return definitions.get(key.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isGroup(String name) {
        return groups.containsKey(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> group(String name) {
        return groups.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    @Override
    public List<String> categoryMaterials(String categoryKey) {
        return categories.getOrDefault(categoryKey.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    @Override
    public String variantType(BlockType blockType) {
        String id = blockType.id().toLowerCase(Locale.ROOT);
        if (id.endsWith("_stairs"))
            return "stairs";
        if (id.endsWith("_slab"))
            return "slab";
        if (id.endsWith("_wall"))
            return "wall";
        if (id.endsWith("_fence_gate"))
            return "fence_gate";
        if (id.endsWith("_fence"))
            return "fence";
        if (id.endsWith("_bars"))
            return "bars";
        if (id.endsWith("_grate"))
            return "grate";
        return "block";
    }

    @Override
    public BlockType mapVariant(BlockType source, BlockFamily target) {
        String variantType = variantType(source);

        if (isVerticalConnector(variantType)) {
            BlockType targetVariant = findVerticalConnectorVariant(target);
            if (targetVariant == null) {
                targetVariant = target.getVariant("block");
            }
            return targetVariant;
        }

        if (variantType.equals("fence_gate")) {
            return target.getVariant("fence_gate");
        }
        if (variantType.equals("grate")) {
            return target.getVariant("grate");
        }

        BlockType targetVariant = target.getVariant(variantType);
        if (targetVariant == null) {
            targetVariant = target.getVariant("block");
        }
        return targetVariant;
    }

    @Override
    public void register(String key, MaterialDefinition definition) {
        definitions.put(key.toLowerCase(Locale.ROOT), definition);
    }

    @Override
    public void reload() {
        // In-memory registry has no backing source.
    }

    /** Populate this registry from a parsed {@code block-families.yml} document. */
    public void loadFrom(ConfigurationSection root) {
        definitions.clear();
        groups.clear();
        categories.clear();

        ConfigurationSection families = root.getConfigurationSection("families");
        if (families != null) {
            for (String key : families.getKeys(false)) {
                ConfigurationSection section = families.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                List<String> variants = section.getStringList("variants");
                String block = section.getString("block", key);
                String prefix = section.getString("prefix", key);
                String barsName = section.getString("bars", null);
                String grateName = section.getString("grate", null);
                boolean hasBars = variants.contains("bars") || barsName != null;

                definitions.put(key.toLowerCase(Locale.ROOT), new MaterialDefinition(
                        block, prefix,
                        variants.contains("stairs"), variants.contains("slab"), variants.contains("wall"),
                        variants.contains("fence"), variants.contains("fence_gate"),
                        hasBars, grateName != null, grateName, barsName));
            }
        }

        ConfigurationSection groupSection = root.getConfigurationSection("groups");
        if (groupSection != null) {
            for (String key : groupSection.getKeys(false)) {
                groups.put(key.toLowerCase(Locale.ROOT), groupSection.getStringList(key));
            }
        }

        ConfigurationSection categorySection = root.getConfigurationSection("categories");
        if (categorySection != null) {
            for (String key : categorySection.getKeys(false)) {
                categories.put(key.toLowerCase(Locale.ROOT), categorySection.getStringList(key));
            }
        }
    }

    private static boolean isVerticalConnector(String variantType) {
        return variantType.equals("wall") || variantType.equals("fence") || variantType.equals("bars");
    }

    private static BlockType findVerticalConnectorVariant(BlockFamily target) {
        BlockType variant = target.getVariant("wall");
        if (variant != null)
            return variant;
        variant = target.getVariant("fence");
        if (variant != null)
            return variant;
        variant = target.getVariant("bars");
        if (variant != null)
            return variant;

        String base = target.getBaseMaterial();
        variant = BlockTypes.get("minecraft:" + base + "_wall");
        if (variant != null)
            return variant;
        variant = BlockTypes.get("minecraft:" + base + "_fence");
        if (variant != null)
            return variant;
        return BlockTypes.get("minecraft:" + base + "_bars");
    }
}
