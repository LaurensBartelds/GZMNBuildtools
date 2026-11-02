package nl.gzmn.gZMNBuildtools.modules.terrainbrush.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.command.TerrainBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.TerrainBrushType;
import nl.gzmn.gZMNBuildtools.shared.menu.Items;
import nl.gzmn.gZMNBuildtools.shared.menu.Menu;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tunes one terrain brush's radius, parameters and pattern, then binds it. */
public class TerrainBrushParamsMenu extends Menu {

    private static final int[] PARAM_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final Menu parent;
    private final TerrainBrushCommand command;
    private final TerrainBrushType type;
    private final Map<String, Double> values = new LinkedHashMap<>();
    private final List<String> patterns = new ArrayList<>();
    private int radius = 5;

    public TerrainBrushParamsMenu(Player viewer, Menu parent, TerrainBrushCommand command, TerrainBrushType type) {
        super(viewer, 5, Component.text("Brush: " + TerrainBrushMenu.capitalise(type.id())));
        this.parent = parent;
        this.command = command;
        this.type = type;
        type.params().forEach(p -> values.put(p.name(), p.defaultValue()));
    }

    /** Sensible click step for a parameter: whole numbers for counts, finer for fractions. */
    static double step(BrushParam p) {
        if (p.max() - p.min() <= 1) {
            return 0.05;
        }
        boolean whole = p.min() == Math.rint(p.min()) && p.defaultValue() == Math.rint(p.defaultValue())
                && p.max() >= 8;
        return whole ? 1 : 0.1;
    }

    @Override
    protected void render() {
        item(4, Items.icon(TerrainBrushMenu.icon(type), TerrainBrushMenu.capitalise(type.id()), NamedTextColor.AQUA,
                type.description()));
        button(10, Items.icon(Material.SLIME_BALL, "Radius: " + radius, NamedTextColor.YELLOW,
                "Left +1, right -1, shift ±5."), e -> {
            radius = clamp(radius + (e.isRightClick() ? -1 : 1) * (e.isShiftClick() ? 5 : 1), 1, command.maxRadius(viewer));
            refresh();
        });
        List<BrushParam> params = type.params();
        for (int i = 0; i < Math.min(params.size(), PARAM_SLOTS.length); i++) {
            BrushParam p = params.get(i);
            if (p.isChoice()) {
                button(PARAM_SLOTS[i], Items.icon(Material.NAME_TAG, p.name() + ": " + p.display(values.get(p.name())),
                        NamedTextColor.YELLOW, p.description(), String.join(", ", p.choices()), "Click to cycle"),
                        e -> {
                            int next = Math.floorMod((int) Math.round(values.get(p.name())) + (e.isRightClick() ? -1 : 1),
                                    p.choices().size());
                            values.put(p.name(), (double) next);
                            refresh();
                        });
                continue;
            }
            double step = step(p);
            button(PARAM_SLOTS[i], Items.icon(Material.COMPARATOR,
                    p.name() + ": " + ParamSpec.format(round(values.get(p.name()))), NamedTextColor.YELLOW,
                    p.description(), "Left +" + ParamSpec.format(step) + ", right -" + ParamSpec.format(step)
                            + ", shift x5", "Range " + ParamSpec.format(p.min()) + " to " + ParamSpec.format(p.max())),
                    e -> {
                        double delta = (e.isRightClick() ? -step : step) * (e.isShiftClick() ? 5 : 1);
                        values.put(p.name(), Math.max(p.min(), Math.min(p.max(), round(values.get(p.name()) + delta))));
                        refresh();
                    });
        }
        int slots = type.patternSlots();
        for (int i = 0; i < slots; i++) {
            String label = type.patternNames().get(i);
            boolean set = i < patterns.size();
            ItemStack icon = set
                    ? Items.icon(Items.blockItem(patterns.get(i)), label + ": " + patterns.get(i), NamedTextColor.GREEN,
                    "Click a block in your inventory to change it.")
                    : Items.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, label + ": not set", NamedTextColor.RED,
                    "Click a block in your inventory to use it.");
            item(28 + i, icon);
        }
        button(36, Items.icon(Material.ARROW, "Back", NamedTextColor.YELLOW), e -> parent.open());
        boolean ready = patterns.size() >= slots;
        button(40, Items.icon(ready ? Material.EMERALD_BLOCK : Material.GRAY_CONCRETE, "Bind to held item",
                ready ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                ready ? "Binds this brush to the item in your hand." : "Choose the block(s) first."),
                e -> {
                    if (patterns.size() < slots) {
                        return;
                    }
                    viewer.closeInventory();
                    command.bind(viewer, type.id(), radius, arguments());
                });
    }

    String arguments() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < type.patternSlots(); i++) {
            sb.append(patterns.get(i)).append(' ');
        }
        for (BrushParam p : type.params()) {
            sb.append(p.name()).append('=').append(p.isChoice() ? p.display(values.get(p.name()))
                    : ParamSpec.format(round(values.get(p.name())))).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    protected void clickBottom(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (type.patternSlots() == 0 || clicked == null || !clicked.getType().isBlock() || clicked.getType().isAir()) {
            return;
        }
        String id = clicked.getType().getKey().getKey();
        if (patterns.size() < type.patternSlots()) {
            patterns.add(id);
        } else {
            // All slots filled: cycle, replacing the oldest choice.
            patterns.remove(0);
            patterns.add(id);
        }
        refresh();
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
