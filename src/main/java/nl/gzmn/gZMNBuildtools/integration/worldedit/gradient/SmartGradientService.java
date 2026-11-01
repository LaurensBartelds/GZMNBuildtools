package nl.gzmn.gZMNBuildtools.integration.worldedit.gradient;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import nl.gzmn.gZMNBuildtools.gradient.smart.BlockCatalog;
import nl.gzmn.gZMNBuildtools.gradient.smart.BlockRules;
import nl.gzmn.gZMNBuildtools.gradient.smart.BuildScan;
import nl.gzmn.gZMNBuildtools.gradient.smart.BuildScanner;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientFrame;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientPainter;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientSettings;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientSettingsStore;
import nl.gzmn.gZMNBuildtools.gradient.smart.Palette;
import nl.gzmn.gZMNBuildtools.gradient.smart.PaletteSpec;
import nl.gzmn.gZMNBuildtools.gradient.smart.ShapeKind;
import nl.gzmn.gZMNBuildtools.gradient.smart.TargetPolicy;
import nl.gzmn.gZMNBuildtools.gradient.smart.VariantResolver;
import nl.gzmn.gZMNBuildtools.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.palette.BlockFilters;
import nl.gzmn.gZMNBuildtools.palette.PaletteGenerator;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Runs the smart gradient against a FAWE edit session: scans the clicked
 * build, remembers it per player so later strokes line up, resolves the
 * palette and writes each change with the original block's state carried over.
 */
public final class SmartGradientService {

    /** Receives a change instead of writing it (used for previews). */
    @FunctionalInterface
    public interface ChangeSink {
        void accept(int x, int y, int z, BlockState state);
    }

    /** What a paint pass did, for player feedback. */
    public record Outcome(int painted, int unchanged, BuildScan scan, GradientFrame frame, Palette palette,
                          boolean reused, String problem) {
        public static Outcome failed(String problem) {
            return new Outcome(0, 0, null, null, null, false, problem);
        }

        public boolean ok() {
            return problem == null;
        }
    }

    /** The build last painted by a player. */
    private record Memory(String world, BuildScan scan, Palette palette, GradientFrame frame,
                          GradientSettings settings, Set<String> groups, long time) {
    }

    private final BlockColors colors;
    private final BlockCatalog catalog;
    private final BlockRules rules;
    private final VariantResolver variants;
    private final GradientPainter painter;
    private final PresetRegistry presets;
    private final GradientSettingsStore settings = new GradientSettingsStore();
    private final Map<UUID, Memory> memory = new ConcurrentHashMap<>();
    private final Map<String, BlockType> types = new ConcurrentHashMap<>();

    private volatile PaletteGenerator generator;
    private volatile int maxScanBlocks = 200_000;
    private volatile int maxScanRadius = 128;
    private volatile long cacheMillis = 10 * 60_000L;
    private volatile int maxRadius = 32;

    public SmartGradientService(PresetRegistry presets) {
        this.presets = presets;
        this.colors = BlockColors.bundled();
        Set<String> ids = BlockType.REGISTRY.values().stream()
                .map(t -> t.id().startsWith("minecraft:") ? t.id().substring(10) : t.id())
                .collect(Collectors.toSet());
        this.catalog = new BlockCatalog(ids, id -> material(id).map(BlockMaterial::isFullCube).orElse(false));
        this.rules = new BlockRules(catalog, this::isDetail);
        this.variants = new VariantResolver(catalog, colors);
        this.painter = new GradientPainter(rules, variants);
        this.generator = new PaletteGenerator(colors, eligible(BlockFilters.DEFAULT_PALETTE_EXCLUDES));
    }

    public void reload(FileConfiguration config) {
        maxScanBlocks = Math.max(100, config.getInt("smart-gradient.max-scan-blocks", 200_000));
        maxScanRadius = Math.max(8, config.getInt("smart-gradient.max-scan-radius", 128));
        cacheMillis = Math.max(1, config.getInt("smart-gradient.cache-minutes", 10)) * 60_000L;
        maxRadius = Math.max(1, config.getInt("smart-gradient.max-radius", 32));
        List<String> excludes = config.isList("palettes.exclude")
                ? config.getStringList("palettes.exclude") : BlockFilters.DEFAULT_PALETTE_EXCLUDES;
        generator = new PaletteGenerator(colors, eligible(excludes));
        memory.clear();
    }

    private Optional<BlockMaterial> material(String id) {
        return Optional.ofNullable(type(id)).map(BlockType::getMaterial);
    }

    BlockType type(String id) {
        return types.computeIfAbsent(id, k -> BlockTypes.get("minecraft:" + k));
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

    public GradientSettingsStore settings() {
        return settings;
    }

    public PaletteGenerator generator() {
        return generator;
    }

    public BlockCatalog catalog() {
        return catalog;
    }

    public BlockColors colors() {
        return colors;
    }

    public int maxRadius() {
        return maxRadius;
    }

    public PaletteSpec parseSpec(String text) {
        return PaletteSpec.parse(text, catalog::exists);
    }

    public PaletteSpec.Context context(String buildMaterial) {
        PaletteGenerator gen = generator;
        return new PaletteSpec.Context() {
            @Override
            public PaletteGenerator generator() {
                return gen;
            }

            @Override
            public Optional<List<String>> preset(String id) {
                return presets.get(id).map(SmartGradientService::presetBlocks);
            }

            @Override
            public String buildMaterial() {
                return buildMaterial;
            }
        };
    }

    private static List<String> presetBlocks(GradientPreset preset) {
        return preset.getBlockIds().stream().map(BlockColors::normalize).toList();
    }

    /** Forget the remembered build so the next stroke rescans. */
    public void forget(UUID player) {
        memory.remove(player);
    }

    /**
     * One paint pass. Call off the main thread with an edit session.
     *
     * @param radius brush radius, or null to paint the whole build
     * @param sink   where changes go; null writes them to the edit session
     */
    public Outcome paint(EditSession editSession, String world, UUID player, BlockVector3 click, Double radius,
                         GradientSettings s, ChangeSink sink) {
        ExtentBlockSource source = new ExtentBlockSource(editSession);
        String clicked = source.blockAt(click.x(), click.y(), click.z());
        if (clicked == null) {
            return Outcome.failed("Aim at a block of the build you want to paint.");
        }

        Memory remembered = memory.get(player);
        boolean reuse = remembered != null && remembered.world.equals(world) && remembered.settings == s
                && System.currentTimeMillis() - remembered.time < cacheMillis
                && remembered.scan.contains(click.x(), click.y(), click.z());
        Memory current;
        if (reuse) {
            current = remembered;
        } else {
            try {
                current = scan(source, world, click, clicked, s, remembered);
            } catch (IllegalArgumentException e) {
                return Outcome.failed(e.getMessage());
            }
            if (current.scan.size() == 0) {
                return Outcome.failed("That block is not part of a build this brush can paint ("
                        + clicked + " is a detail or terrain block). Try target=all.");
            }
        }
        memory.put(player, new Memory(current.world, current.scan, current.palette, current.frame, s,
                current.groups, System.currentTimeMillis()));

        TargetPolicy policy = new TargetPolicy(s.target(), rules, current.groups);
        GradientPainter.StrokeFilter stroke = radius == null ? GradientPainter.StrokeFilter.ALL
                : GradientPainter.StrokeFilter.sphere(click.x(), click.y(), click.z(), radius);
        GradientPainter.Result result = painter.paint(source, current.scan, policy, current.palette, current.frame,
                s, stroke, (x, y, z, from, to) -> {
                    BlockType target = type(to);
                    if (target == null) {
                        return;
                    }
                    BlockState state = StateMapper.map(editSession.getBlock(x, y, z), target);
                    if (sink != null) {
                        sink.accept(x, y, z, state);
                    } else {
                        editSession.setBlock(x, y, z, state);
                    }
                });
        return new Outcome(result.painted(), result.unchanged(), current.scan, current.frame, current.palette,
                reuse, null);
    }

    private Memory scan(ExtentBlockSource source, String world, BlockVector3 click, String clicked,
                        GradientSettings s, Memory previous) {
        PaletteSpec spec = parseSpec(s.palette());
        Palette fixed = spec.dependsOnBuild() ? null : spec.resolve(context(null), s.steps());
        // Keep the material groups of the build painted last, so rescanning a
        // half-painted build still finds the unpainted part.
        Set<String> extra = new HashSet<>();
        if (previous != null && previous.world.equals(world)
                && previous.scan.bounds().contains(click.x(), click.y(), click.z())) {
            extra.addAll(previous.groups);
        }
        TargetPolicy policy = painter.policy(s.target(), clicked, fixed, extra);
        BuildScan scan = BuildScanner.scan(source, click.x(), click.y(), click.z(), policy, rules,
                maxScanBlocks, maxScanRadius);
        Palette palette = fixed;
        if (palette == null) {
            boolean samePalette = previous != null && previous.palette != null
                    && previous.settings.palette().equals(s.palette()) && previous.settings.steps() == s.steps()
                    && !extra.isEmpty();
            palette = samePalette ? previous.palette
                    : spec.resolve(context(scan.mainGroup()), s.steps());
        }
        Set<String> groups = new HashSet<>(policy.targetGroups());
        palette.blocks().forEach(b -> groups.add(rules.group(b)));
        GradientFrame frame = GradientFrame.of(scan.bounds(), s.axis(), s.range());
        return new Memory(world, scan, palette, frame, s, groups, System.currentTimeMillis());
    }

    /** Readable material list of a scan, most common first. */
    public static String describeMaterials(BuildScan scan, int limit) {
        int total = scan.groups().values().stream().mapToInt(Integer::intValue).sum();
        return scan.rankedGroups().stream().limit(limit)
                .map(e -> e.getKey() + " " + Math.round(100.0 * e.getValue() / Math.max(1, total)) + "%")
                .collect(Collectors.joining(", "));
    }

    public static String axisName(GradientFrame frame) {
        return frame.axis().id().toLowerCase(Locale.ROOT);
    }
}
