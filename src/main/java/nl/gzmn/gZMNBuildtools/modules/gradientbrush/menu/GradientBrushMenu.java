package nl.gzmn.gZMNBuildtools.modules.gradientbrush.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.command.GradientBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.Dither;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientAxis;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientSettings;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.Palette;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.PaletteSpec;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.TargetMode;
import nl.gzmn.gZMNBuildtools.shared.menu.Items;
import nl.gzmn.gZMNBuildtools.shared.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * The smart gradient brush menu: shows the palette, builds one by clicking
 * blocks in your own inventory, and toggles every option. Edits apply to the
 * bound brush straight away.
 */
public class GradientBrushMenu extends Menu {

    private final GradientBrushCommand command;
    private final SmartGradientService service;
    private final PresetRegistry presets;

    public GradientBrushMenu(Player viewer, GradientBrushCommand command, PresetRegistry presets) {
        super(viewer, 6, Component.text("Smart Gradient Brush"));
        this.command = command;
        this.service = command.service();
        this.presets = presets;
    }

    private GradientSettings settings() {
        return service.settings().get(viewer.getUniqueId());
    }

    private void update(UnaryOperator<GradientSettings> change) {
        service.settings().update(viewer.getUniqueId(), change);
        refresh();
    }

    @Override
    protected void render() {
        GradientSettings s = settings();
        renderPalette(s);

        button(18, Items.icon(Material.ENDER_EYE, "Auto palette", NamedTextColor.LIGHT_PURPLE,
                "Builds an aged ramp from the material", "of whatever build you click."),
                e -> update(x -> x.withPalette("auto")));
        button(19, Items.icon(Material.BOOKSHELF, "Presets", NamedTextColor.GOLD,
                "Pick one of the gradient presets."), e -> new PresetMenu(viewer, this, presets, service).open());
        button(20, Items.icon(Material.REPEATER, "Steps: " + (s.steps() == 0 ? "auto" : s.steps()),
                NamedTextColor.YELLOW, "Left +1, right -1.", "Generated palettes and short block",
                "lists are filled in to this many steps."),
                e -> update(x -> x.withSteps(Math.max(0, Math.min(16, x.steps() + (e.isRightClick() ? -1 : 1))))));
        button(21, Items.icon(Material.HOPPER, "Remove last step", NamedTextColor.YELLOW,
                "For palettes you build from blocks."), e -> removeLastStep());
        button(22, Items.icon(Material.CLOCK, "Flip direction", NamedTextColor.YELLOW,
                "Run the gradient the other way."), e -> update(x -> x.withAxis(flip(x.axis()))));

        button(27, Items.icon(Material.COMPASS, "Direction: " + s.axis().id(), NamedTextColor.AQUA,
                "auto: up for walls and towers,", "along the length for floors.", "Click to cycle."),
                e -> update(x -> x.withAxis(cycle(GradientAxis.values(), x.axis(), e.isRightClick()))));
        button(28, Items.icon(Material.TARGET, "Paint: " + s.target().id(), NamedTextColor.AQUA,
                describe(s.target()), "Click to cycle."),
                e -> update(x -> x.withTarget(cycle(TargetMode.values(), x.target(), e.isRightClick()))));
        button(29, Items.icon(Material.GRAVEL, "Blend: " + s.dither().name().toLowerCase(Locale.ROOT),
                NamedTextColor.AQUA, "ordered: even and grain-free", "noise: random speckle",
                "none: clean bands", "Click to cycle."),
                e -> update(x -> x.withDither(cycle(Dither.values(), x.dither(), e.isRightClick()))));
        button(30, Items.icon(Material.FEATHER, "Softness: " + Math.round(s.softness() * 100) + "%",
                NamedTextColor.AQUA, "How far each step blends into the next.", "Left +10%, right -10%."),
                e -> update(x -> x.withSoftness(Math.round((x.softness() + (e.isRightClick() ? -0.1 : 0.1)) * 10) / 10.0)));
        button(31, Items.icon(s.keepShapes() ? Material.STONE_BRICK_STAIRS : Material.STONE_BRICKS,
                "Shapes: " + (s.keepShapes() ? "keep" : "full blocks"), NamedTextColor.AQUA,
                "keep: stairs stay stairs, slabs stay slabs.", "Click to toggle."),
                e -> update(x -> x.withKeepShapes(!x.keepShapes())));
        button(32, Items.icon(Material.SLIME_BALL, "Radius: " + s.radius(), NamedTextColor.AQUA,
                "Brush stroke size.", "Left +1, right -1, shift ±5."),
                e -> {
                    update(x -> x.withRadius(Math.max(1, Math.min(service.maxRadius(),
                            x.radius() + (e.isRightClick() ? -1 : 1) * (e.isShiftClick() ? 5 : 1)))));
                    command.syncBrushSize(viewer);
                });

        button(45, Items.icon(Material.BLAZE_ROD, "Bind to held item", NamedTextColor.GREEN,
                "Binds the brush to the item in your hand.", "Then click builds to paint them."),
                e -> run(() -> command.bind(viewer, "")));
        button(46, Items.icon(Material.PAINTING, "Paint whole build", NamedTextColor.GREEN,
                "Paints the build you are looking at."), e -> run(() -> command.paint(viewer, "")));
        button(47, Items.icon(Material.SPYGLASS, "Preview whole build", NamedTextColor.GREEN,
                "Shows the paint as ghost blocks first."), e -> run(() -> command.preview(viewer, "")));
        button(48, Items.icon(Material.BOOK, "Inspect build", NamedTextColor.GREEN,
                "Size, materials and direction of the", "build you are looking at."),
                e -> run(() -> command.info(viewer)));
        button(50, Items.icon(Material.WRITABLE_BOOK, "Web editor", NamedTextColor.GREEN,
                "Get a personal link to design the", "gradient in your browser."),
                e -> run(() -> viewer.performCommand("gb web")));
        button(52, Items.icon(Material.LAVA_BUCKET, "Reset", NamedTextColor.RED, "Back to the default settings."),
                e -> {
                    service.settings().reset(viewer.getUniqueId());
                    refresh();
                });
        button(53, Items.icon(Material.BARRIER, "Close", NamedTextColor.RED), e -> viewer.closeInventory());
    }

    private void renderPalette(GradientSettings s) {
        try {
            PaletteSpec spec = service.parseSpec(s.palette());
            if (spec.dependsOnBuild()) {
                item(4, Items.icon(Material.ENDER_EYE, "Palette: auto", NamedTextColor.LIGHT_PURPLE,
                        "Made from the build you click.", "Click a block in your inventory to",
                        "base the palette on it instead;", "shift-click blocks to build your own."));
                return;
            }
            Palette palette = spec.resolve(service.context(null), s.steps());
            List<Palette.Stop> stops = palette.stops();
            int shown = Math.min(9, stops.size());
            for (int i = 0; i < shown; i++) {
                int index = i * stops.size() / shown;
                Palette.Stop stop = stops.get(index);
                String block = stop.blocks().get(0).block();
                int rgb = service.colors().get(block).map(c -> c.sideRgb()).orElse(0xFFFFFF);
                ItemStack icon = Items.icon(Items.blockItem(block),
                        Component.text("Step " + (index + 1) + ": " + stop, TextColor.color(rgb)),
                        index == 0 ? "Start of the gradient" : index == stops.size() - 1 ? "End of the gradient" : "",
                        "Palette: " + s.palette());
                item(i, icon);
            }
            item(9 + 4, Items.icon(Material.NAME_TAG, "Palette: " + s.palette(), NamedTextColor.WHITE,
                    "Click a block in your inventory: palette from it", "Shift-click blocks: build your own, in order"));
        } catch (IllegalArgumentException e) {
            item(4, Items.icon(Material.BARRIER, "Invalid palette", NamedTextColor.RED, e.getMessage()));
        }
    }

    /** Click a block in your own inventory: a palette from it; shift-click: append it as the next step. */
    @Override
    protected void clickBottom(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.getType().isBlock() || clicked.getType().isAir()) {
            return;
        }
        String id = clicked.getType().getKey().getKey();
        if (!service.catalog().exists(id)) {
            return;
        }
        if (event.isShiftClick()) {
            String current = settings().palette();
            boolean list = isSimpleList(current);
            update(x -> x.withPalette(list ? current + "," + id : id));
        } else {
            boolean worn = service.colors().get("mossy_" + id).isPresent()
                    || service.colors().get("cracked_" + id).isPresent();
            update(x -> x.withPalette((worn ? "aged:" : "shades:") + id));
        }
    }

    private boolean isSimpleList(String spec) {
        try {
            return service.parseSpec(spec).kind() == PaletteSpec.Kind.LIST && !spec.contains("[");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void removeLastStep() {
        String current = settings().palette();
        if (!isSimpleList(current) || !current.contains(",")) {
            return;
        }
        update(x -> x.withPalette(current.substring(0, current.lastIndexOf(','))));
    }

    private void run(Runnable action) {
        viewer.closeInventory();
        action.run();
    }

    private static GradientAxis flip(GradientAxis axis) {
        return switch (axis) {
            case UP -> GradientAxis.DOWN;
            case DOWN -> GradientAxis.UP;
            case X -> GradientAxis.NEG_X;
            case NEG_X -> GradientAxis.X;
            case Z -> GradientAxis.NEG_Z;
            case NEG_Z -> GradientAxis.Z;
            case RADIAL -> GradientAxis.INWARD;
            case INWARD -> GradientAxis.RADIAL;
            case AUTO -> GradientAxis.DOWN;
        };
    }

    static <T extends Enum<T>> T cycle(T[] values, T current, boolean backwards) {
        int i = Arrays.asList(values).indexOf(current);
        return values[Math.floorMod(i + (backwards ? -1 : 1), values.length)];
    }

    private static String describe(TargetMode mode) {
        return switch (mode) {
            case CLICKED -> "Only the material you click.";
            case MAIN -> "The build's main materials.";
            case STRUCTURAL -> "All building blocks of the build.";
            case ALL -> "Every block of the build.";
        };
    }
}
