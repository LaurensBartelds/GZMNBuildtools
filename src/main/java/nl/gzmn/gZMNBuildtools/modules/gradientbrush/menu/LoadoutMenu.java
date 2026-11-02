package nl.gzmn.gZMNBuildtools.modules.gradientbrush.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.command.GradientBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.GradientSettings;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.PaletteSpec;
import nl.gzmn.gZMNBuildtools.shared.menu.Items;
import nl.gzmn.gZMNBuildtools.shared.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/** Saved gradient loadouts: click to load, shift-click to delete, or save the current brush. */
public class LoadoutMenu extends Menu {

    private final Menu parent;
    private final GradientBrushCommand command;
    private final SmartGradientService service;

    public LoadoutMenu(Player viewer, Menu parent, GradientBrushCommand command) {
        super(viewer, 6, Component.text("Gradient Loadouts"));
        this.parent = parent;
        this.command = command;
        this.service = command.service();
    }

    @Override
    protected void render() {
        List<String> names = service.loadoutNames(viewer.getUniqueId());
        for (int i = 0; i < Math.min(45, names.size()); i++) {
            String name = names.get(i);
            Optional<GradientSettings> saved = service.loadout(viewer.getUniqueId(), name);
            Material icon = saved.map(this::iconFor).orElse(Material.PAPER);
            button(i, Items.icon(icon, name, NamedTextColor.AQUA,
                    saved.map(GradientSettings::palette).orElse("?"), "Click to load, shift-click to delete"),
                    e -> {
                        if (e.isShiftClick()) {
                            command.deleteLoadout(viewer, name);
                            refresh();
                        } else {
                            viewer.closeInventory();
                            command.loadLoadout(viewer, name);
                        }
                    });
        }
        button(48, Items.icon(Material.WRITABLE_BOOK, "Save current brush", NamedTextColor.GREEN,
                "Saves as " + service.nextLoadoutName(viewer.getUniqueId()) + ".",
                "Use /gb save <name> to pick a name."), e -> {
            command.saveLoadout(viewer, null);
            refresh();
        });
        button(49, Items.icon(Material.ARROW, "Back", NamedTextColor.YELLOW), e -> parent.open());
    }

    private Material iconFor(GradientSettings settings) {
        try {
            PaletteSpec spec = service.parseSpec(settings.palette());
            if (spec.dependsOnBuild()) {
                return Material.ENDER_EYE;
            }
            return Items.blockItem(spec.resolve(service.context(null), settings.steps()).stops().get(0)
                    .blocks().get(0).block());
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }
}
