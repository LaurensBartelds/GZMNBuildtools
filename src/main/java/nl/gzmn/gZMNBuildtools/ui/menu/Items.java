package nl.gzmn.gZMNBuildtools.ui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Builds menu icons with readable (non-italic) names and lore. */
public final class Items {

    private Items() {
    }

    public static ItemStack icon(Material material, Component name, String... lore) {
        ItemStack item = new ItemStack(material.isItem() ? material : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack icon(Material material, String name, NamedTextColor color, String... lore) {
        return icon(material, Component.text(name, color), lore);
    }

    /** The item form of a block id, or paper for blocks without one. */
    public static Material blockItem(String blockId) {
        Material m = Material.matchMaterial(blockId);
        return m != null && m.isItem() ? m : Material.PAPER;
    }
}
