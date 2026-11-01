package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.command.Swatches;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientFrame;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientPainter;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientSettings;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientSettingsStore;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.Palette;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.PaletteSpec;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.fawe.SmartGradientBrush;
import nl.gzmn.gZMNBuildtools.shared.build.BlockCatalog;
import nl.gzmn.gZMNBuildtools.shared.build.BlockRules;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScan;
import nl.gzmn.gZMNBuildtools.shared.build.BuildScanner;
import nl.gzmn.gZMNBuildtools.shared.build.ShapeKind;
import nl.gzmn.gZMNBuildtools.shared.build.TargetPolicy;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.shared.palette.MaterialClass;
import nl.gzmn.gZMNBuildtools.shared.palette.PaletteGenerator;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.shared.worldedit.ExtentBlockSource;
import nl.gzmn.gZMNBuildtools.shared.worldedit.StateMapper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private final BlockKnowledge blocks;
    private final BlockColors colors;
    private final BlockCatalog catalog;
    private final BlockRules rules;
    private final GradientPainter painter;
    private final PresetRegistry presets;
    private final GradientSettingsStore settings = new GradientSettingsStore();
    private final Map<UUID, Memory> memory = new ConcurrentHashMap<>();

    private volatile long cacheMillis = 10 * 60_000L;
    private volatile int maxRadius = 32;

    public SmartGradientService(PresetRegistry presets, BlockKnowledge blocks) {
        this.presets = presets;
        this.blocks = blocks;
        this.colors = blocks.colors();
        this.catalog = blocks.catalog();
        this.rules = blocks.rules();
        this.painter = new GradientPainter(rules, blocks.variants());
    }

    public void reload(FileConfiguration config) {
        cacheMillis = Math.max(1, config.getInt("smart-gradient.cache-minutes", 10)) * 60_000L;
        maxRadius = Math.max(1, config.getInt("smart-gradient.max-radius", 32));
        memory.clear();
    }

    public GradientSettingsStore settings() {
        return settings;
    }

    public PaletteGenerator generator() {
        return blocks.generator();
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

    // ---- module API (used by other modules, e.g. the web editor) ------------------------------------------------------

    private volatile WebEditorLink webEditor;

    /** Set by the web editor module while it runs; null when it is off. */
    public void setWebEditor(WebEditorLink link) {
        this.webEditor = link;
    }

    public Optional<WebEditorLink> webEditor() {
        return Optional.ofNullable(webEditor);
    }

    /** Names of the options {@link #applyOptions} accepts. */
    public List<String> optionNames() {
        return GradientSettings.OPTIONS;
    }

    /** The blocks automatic palettes may use, with their colours and shape variants. */
    public List<PaletteBlock> paletteBlocks() {
        List<PaletteBlock> out = new ArrayList<>();
        for (String id : blocks.generator().candidates()) {
            BlockColors.Entry entry = colors.get(id).orElseThrow();
            out.add(new PaletteBlock(id, entry.sideRgb(), entry.topRgb(),
                    MaterialClass.of(id).name().toLowerCase(Locale.ROOT),
                    catalog.variant(id, ShapeKind.STAIRS).isPresent(), catalog.variant(id, ShapeKind.SLAB).isPresent()));
        }
        return out;
    }

    /**
     * Resolves a palette text for display; {@code material} stands in for the
     * build when previewing {@code auto}.
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public PaletteView previewPalette(String spec, int steps, String material) {
        Palette palette = parseSpec(spec).resolve(context(material == null ? null : BlockColors.normalize(material)),
                steps);
        List<List<PaletteView.Entry>> stops = new ArrayList<>();
        for (Palette.Stop stop : palette.stops()) {
            stops.add(stop.blocks().stream()
                    .map(w -> new PaletteView.Entry(w.block(), w.weight(),
                            colors.get(w.block()).map(BlockColors.Entry::sideRgb).orElse(0x888888)))
                    .toList());
        }
        return new PaletteView(stops, palette.toString());
    }

    public SettingsView settingsView(UUID player) {
        GradientSettings s = settings.get(player);
        Map<String, String> d = s.describe();
        return new SettingsView(s.palette(), s.steps(), d.get("axis"), d.get("target"), d.get("dither"),
                s.softness(), d.get("shapes"), s.radius());
    }

    /**
     * Changes a player's palette and options at once (all or nothing).
     *
     * @param palette new palette text, or null to keep the current one
     * @throws IllegalArgumentException with a player-facing message
     */
    public void applyOptions(UUID player, String palette, Map<String, String> options) {
        GradientSettings s = settings.get(player);
        if (palette != null) {
            parseSpec(palette);
            s = s.withPalette(palette);
        }
        for (Map.Entry<String, String> option : options.entrySet()) {
            s = s.with(option.getKey(), option.getValue(), maxRadius);
        }
        settings.set(player, s);
        forget(player);
    }

    /** The player's palette as coloured blocks (or "auto"), for chat. */
    public Component paletteSwatches(UUID player) {
        GradientSettings s = settings.get(player);
        PaletteSpec spec = parseSpec(s.palette());
        if (spec.dependsOnBuild()) {
            return Component.text("auto");
        }
        return Swatches.of(spec.resolve(context(null), s.steps()), colors);
    }

    /** Resizes the player's bound smart gradient brush (if they hold it) to their radius setting. */
    public void syncBrushSize(Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
            BrushTool tool = session.getBrushTool(actor, false);
            if (tool != null && tool.getBrush() instanceof SmartGradientBrush) {
                tool.setSize(settings.get(player.getUniqueId()).radius());
            }
        } catch (WorldEditException e) {
            // Not holding a brush tool: the radius applies on the next bind.
        }
    }

    // ---- inside the module -------------------------------------------------------------------------------------------

    public PaletteSpec parseSpec(String text) {
        return PaletteSpec.parse(text, catalog::exists);
    }

    public PaletteSpec.Context context(String buildMaterial) {
        PaletteGenerator gen = blocks.generator();
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
        BuildPainter.StrokeFilter stroke = radius == null ? BuildPainter.StrokeFilter.ALL
                : BuildPainter.StrokeFilter.sphere(click.x(), click.y(), click.z(), radius);
        BuildPainter.Result result = painter.paint(source, current.scan, policy, current.palette, current.frame,
                s, stroke, (x, y, z, from, to) -> {
                    BlockType target = blocks.type(to);
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
                blocks.maxScanBlocks(), blocks.maxScanRadius());
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
