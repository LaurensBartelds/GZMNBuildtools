package nl.gzmn.gZMNBuildtools.ui.brush;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import nl.gzmn.gZMNBuildtools.integration.worldedit.gradient.SmartGradientService;
import nl.gzmn.gZMNBuildtools.ui.menu.Items;
import nl.gzmn.gZMNBuildtools.ui.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Lists the gradient presets; clicking one makes it the brush palette. */
public class PresetMenu extends Menu {

    private final Menu parent;
    private final PresetRegistry presets;
    private final SmartGradientService service;

    public PresetMenu(Player viewer, Menu parent, PresetRegistry presets, SmartGradientService service) {
        super(viewer, 6, Component.text("Gradient Presets"));
        this.parent = parent;
        this.presets = presets;
        this.service = service;
    }

    @Override
    protected void render() {
        List<String> ids = presets.ids().stream().sorted().toList();
        for (int i = 0; i < Math.min(45, ids.size()); i++) {
            String id = ids.get(i);
            GradientPreset preset = presets.get(id).orElseThrow();
            Material icon = preset.getIcon() != null ? preset.getIcon() : Material.PAPER;
            button(i, Items.icon(icon, preset.getDisplayName(), NamedTextColor.GOLD,
                    preset.getDescription(), String.join(" > ", preset.getBlockIds()), "Click to use"),
                    e -> {
                        service.settings().update(viewer.getUniqueId(), s -> s.withPalette("preset:" + id));
                        parent.open();
                    });
        }
        button(49, Items.icon(Material.ARROW, "Back", NamedTextColor.YELLOW), e -> parent.open());
    }
}
