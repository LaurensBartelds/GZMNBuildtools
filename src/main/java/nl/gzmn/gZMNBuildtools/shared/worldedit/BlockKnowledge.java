package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import nl.gzmn.gZMNBuildtools.shared.build.BlockCatalog;
import nl.gzmn.gZMNBuildtools.shared.build.BlockRules;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import nl.gzmn.gZMNBuildtools.shared.build.VariantResolver;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockFilters;
import nl.gzmn.gZMNBuildtools.shared.palette.PaletteGenerator;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * What the plugin knows about the server's blocks at runtime: their shapes and
 * materials (from WorldEdit's registry), colours, which count as protected
 * details, and which may appear in automatic palettes. Shared by every
 * build-aware tool.
 */
public final class BlockKnowledge {

    private final BlockColors colors;
    private final BlockCatalog catalog;
    private final BlockRules rules;
    private final VariantResolver variants;
    private final Map<String, BlockType> types = new ConcurrentHashMap<>();

    private volatile PaletteGenerator generator;
    private volatile int maxScanBlocks = 200_000;
    private volatile int maxScanRadius = 128;

    public BlockKnowledge() {
        this.colors = BlockColors.bundled();
        Set<String> ids = BlockType.REGISTRY.values().stream()
                .map(t -> t.id().startsWith("minecraft:") ? t.id().substring(10) : t.id())
                .collect(Collectors.toSet());
        this.catalog = new BlockCatalog(ids, id -> material(id).map(BlockMaterial::isFullCube).orElse(false));
        this.rules = new BlockRules(catalog, this::isDetail);
        this.variants = new VariantResolver(catalog, colors);
        this.generator = new PaletteGenerator(colors, eligible(BlockFilters.DEFAULT_PALETTE_EXCLUDES));
    }

    public void reload(FileConfiguration config) {
        maxScanBlocks = Math.max(100, config.getInt("build-scan.max-blocks",
                config.getInt("smart-gradient.max-scan-blocks", 200_000)));
        maxScanRadius = Math.max(8, config.getInt("build-scan.max-radius",
                config.getInt("smart-gradient.max-scan-radius", 128)));
        List<String> excludes = config.isList("palettes.exclude")
                ? config.getStringList("palettes.exclude") : BlockFilters.DEFAULT_PALETTE_EXCLUDES;
        generator = new PaletteGenerator(colors, eligible(excludes));
    }

    /** The WorldEdit block type for an id without namespace, or null. */
    public BlockType type(String id) {
        return types.computeIfAbsent(id, k -> BlockTypes.get("minecraft:" + k));
    }

    private Optional<BlockMaterial> material(String id) {
        return Optional.ofNullable(type(id)).map(BlockType::getMaterial);
    }

    /** Full-cube blocks that are details rather than building material. */
    private boolean isDetail(String id) {
        Optional<BlockMaterial> m = material(id);
        if (m.isEmpty()) {
            return true;
        }
        BlockMaterial mat = m.get();
        if (mat.getLightValue() > 0 || mat.isTile() || mat.hasContainer() || mat.isLiquid()) {
            return true;
        }
        return catalog.classify(id).kind() == ShapeKind.FULL && !mat.isOpaque();
    }

    private Predicate<String> eligible(List<String> excludes) {
        Predicate<String> excluded = BlockFilters.anyGlob(excludes);
        return id -> {
            if (excluded.test(id) || type(id) == null) {
                return false;
            }
            BlockMaterial m = type(id).getMaterial();
            return m.isFullCube() && m.isOpaque() && m.getLightValue() == 0 && !m.isTile() && !m.hasContainer()
                    && !m.isPowerSource() && !m.isLiquid() && !m.isAir();
        };
    }

    public BlockColors colors() {
        return colors;
    }

    public BlockCatalog catalog() {
        return catalog;
    }

    public BlockRules rules() {
        return rules;
    }

    public VariantResolver variants() {
        return variants;
    }

    public PaletteGenerator generator() {
        return generator;
    }

    public int maxScanBlocks() {
        return maxScanBlocks;
    }

    public int maxScanRadius() {
        return maxScanRadius;
    }
}
