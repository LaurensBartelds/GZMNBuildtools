package nl.gzmn.gZMNBuildtools.modules.terrainbrush.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.command.TerrainBrushCommand;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.TerrainBrushType;
import nl.gzmn.gZMNBuildtools.shared.menu.Items;
import nl.gzmn.gZMNBuildtools.shared.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Picker for the terrain brushes; each opens its parameter page. */
public class TerrainBrushMenu extends Menu {

    private final TerrainBrushCommand command;

    public TerrainBrushMenu(Player viewer, TerrainBrushCommand command) {
        super(viewer, 3, Component.text("Terrain Brushes"));
        this.command = command;
    }

    static Material icon(TerrainBrushType type) {
        return switch (type) {
            case RAISE -> Material.PISTON;
            case LOWER -> Material.IRON_SHOVEL;
            case FLATTEN -> Material.SMOOTH_STONE_SLAB;
            case SMOOTH -> Material.SNOWBALL;
            case TERRACE -> Material.STONE_BRICK_STAIRS;
            case ROUGHEN -> Material.GRAVEL;
            case OVERLAY -> Material.MOSS_CARPET;
            case BLOB -> Material.MOSSY_COBBLESTONE;
            case SPIKE -> Material.POINTED_DRIPSTONE;
            case TREE -> Material.OAK_SAPLING;
            case ROCK -> Material.COBBLESTONE;
            case SNOW -> Material.SNOW;
        };
    }

    @Override
    protected void render() {
        TerrainBrushType[] types = TerrainBrushType.values();
        for (int i = 0; i < types.length; i++) {
            TerrainBrushType type = types[i];
            button(i < 9 ? 9 + i : i - 9, Items.icon(icon(type), capitalise(type.id()), NamedTextColor.AQUA,
                    type.description(), "Click to set it up"),
                    e -> new TerrainBrushParamsMenu(viewer, this, command, type).open());
        }
        button(22, Items.icon(Material.BARRIER, "Close", NamedTextColor.RED), e -> viewer.closeInventory());
    }

    static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
