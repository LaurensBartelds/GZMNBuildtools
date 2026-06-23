package nl.gzmn.gZMNBuildtools.ui.typereplace;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.typereplace.BlockFamily;
import nl.gzmn.gZMNBuildtools.typereplace.registry.InMemoryBlockFamilyRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * View component for the Type Replace GUI: builds the inventories and items.
 * Holds no per-player state (that lives in {@link TypeReplaceSessions}).
 */
public class TypeReplaceMenu {

    /** A full double-chest inventory (6 rows of 9). */
    private static final int INVENTORY_SIZE = 54;

    public enum MaterialCategory {
        STONE("Stone & Cobblestone", Material.STONE),
        DEEPSLATE("Deepslate", Material.DEEPSLATE),
        BRICKS("Bricks", Material.BRICKS),
        SANDSTONE("Sandstone", Material.SANDSTONE),
        WOOD("Wood", Material.OAK_PLANKS),
        NETHER("Nether", Material.NETHER_BRICKS),
        COPPER("Copper", Material.COPPER_BLOCK),
        PRISMARINE("Prismarine", Material.PRISMARINE),
        QUARTZ("Quartz", Material.QUARTZ_BLOCK),
        END("End & Purpur", Material.PURPUR_BLOCK),
        TUFF("Tuff", Material.TUFF),
        OTHER("Other", Material.IRON_BLOCK);

        final String displayName;
        final Material icon;

        MaterialCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    private BlockFamilyRegistry blockFamilies = InMemoryBlockFamilyRegistry.bundled();

    public void setBlockFamilies(BlockFamilyRegistry blockFamilies) {
        this.blockFamilies = blockFamilies;
    }

    public int categoryCount() {
        return MaterialCategory.values().length;
    }

    public MaterialCategory categoryAt(int ordinal) {
        MaterialCategory[] values = MaterialCategory.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public List<String> materialsFor(MaterialCategory category) {
        return blockFamilies.categoryMaterials(category.name().toLowerCase(Locale.ROOT));
    }

    public Inventory mainMenu(Player player, TypeReplaceSessions.Session builder) {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text("Type Replace").color(NamedTextColor.DARK_AQUA)
                        .decorate(TextDecoration.BOLD));

        fillBackground(inv);

        inv.setItem(0, createInfoItem());

        inv.setItem(8, createSelectionStatusItem(player));

        if (builder.sourceMaterial != null) {
            ItemStack sourceItem = createMaterialDisplayItem(builder.sourceMaterial, "Source");
            inv.setItem(10, sourceItem);
            inv.setItem(11, sourceItem.clone());
            inv.setItem(12, sourceItem.clone());
        } else {
            ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("No source selected").color(NamedTextColor.GRAY));
            empty.setItemMeta(meta);
            inv.setItem(10, empty);
            inv.setItem(11, empty.clone());
            inv.setItem(12, empty.clone());
        }

        ItemStack arrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(Component.text("→").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        arrow.setItemMeta(arrowMeta);
        inv.setItem(13, arrow);

        if (builder.targetMaterial != null) {
            ItemStack targetItem = createMaterialDisplayItem(builder.targetMaterial, "Target");
            inv.setItem(14, targetItem);
            inv.setItem(15, targetItem.clone());
            inv.setItem(16, targetItem.clone());
        } else {
            ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("No target selected").color(NamedTextColor.GRAY));
            empty.setItemMeta(meta);
            inv.setItem(14, empty);
            inv.setItem(15, empty.clone());
            inv.setItem(16, empty.clone());
        }

        ItemStack selectSource = new ItemStack(
                builder.sourceMaterial != null ? Material.LIME_DYE : Material.YELLOW_DYE);
        ItemMeta sourceMeta = selectSource.getItemMeta();
        sourceMeta.displayName(Component.text(builder.sourceMaterial != null ? "Change Source" : "Select Source")
                .color(NamedTextColor.GREEN));
        sourceMeta.lore(Arrays.asList(
                Component.text("Click to select the material").color(NamedTextColor.GRAY),
                Component.text("you want to replace FROM").color(NamedTextColor.GRAY)));
        selectSource.setItemMeta(sourceMeta);
        inv.setItem(37, selectSource);

        ItemStack selectTarget = new ItemStack(
                builder.targetMaterial != null ? Material.LIME_DYE : Material.YELLOW_DYE);
        ItemMeta targetMeta = selectTarget.getItemMeta();
        targetMeta.displayName(Component.text(builder.targetMaterial != null ? "Change Target" : "Select Target")
                .color(NamedTextColor.GREEN));
        targetMeta.lore(Arrays.asList(
                Component.text("Click to select the material").color(NamedTextColor.GRAY),
                Component.text("you want to replace TO").color(NamedTextColor.GRAY)));
        selectTarget.setItemMeta(targetMeta);
        inv.setItem(43, selectTarget);

        boolean canExecute = builder.sourceMaterial != null && builder.targetMaterial != null;
        ItemStack execute = new ItemStack(canExecute ? Material.EMERALD : Material.COAL);
        ItemMeta executeMeta = execute.getItemMeta();
        executeMeta.displayName(Component.text("Execute Replacement")
                .color(canExecute ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decorate(TextDecoration.BOLD));
        if (canExecute) {
            executeMeta.lore(Arrays.asList(
                    Component.text("Click to replace all").color(NamedTextColor.GRAY),
                    Component.text(builder.sourceMaterial + " → " + builder.targetMaterial)
                            .color(NamedTextColor.YELLOW)));
        } else {
            executeMeta.lore(Arrays.asList(
                    Component.text("Select both source and target").color(NamedTextColor.RED),
                    Component.text("materials first").color(NamedTextColor.RED)));
        }
        execute.setItemMeta(executeMeta);
        inv.setItem(49, execute);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

        return inv;
    }

    public Inventory categorySelector(String selecting) {
        String title = "source".equals(selecting) ? "Select Source Category" : "Select Target Category";
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text(title).color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.BOLD));

        fillBackground(inv);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        MaterialCategory[] categories = MaterialCategory.values();
        for (int i = 0; i < categories.length; i++) {
            MaterialCategory category = categories[i];
            ItemStack item = new ItemStack(category.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(category.displayName).color(NamedTextColor.YELLOW));
            meta.lore(Arrays.asList(
                    Component.text(materialsFor(category).size() + " materials")
                            .color(NamedTextColor.GRAY)));
            item.setItemMeta(meta);
            inv.setItem(9 + i, item);
        }

        return inv;
    }

    public Inventory materialSelector(MaterialCategory category, String selecting) {
        String title = "source".equals(selecting) ? "Select Source Material" : "Select Target Material";

        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text(title).color(NamedTextColor.DARK_GREEN)
                        .decorate(TextDecoration.BOLD));

        fillBackground(inv);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Categories").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        ItemStack categoryItem = new ItemStack(category.icon);
        ItemMeta categoryMeta = categoryItem.getItemMeta();
        categoryMeta.displayName(
                Component.text(category.displayName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        categoryItem.setItemMeta(categoryMeta);
        inv.setItem(4, categoryItem);

        List<String> materials = materialsFor(category);
        for (int i = 0; i < Math.min(materials.size(), 36); i++) {
            inv.setItem(9 + i, createMaterialItem(materials.get(i)));
        }

        inv.setItem(53, back.clone());

        return inv;
    }

    /** Fill every slot of an inventory with a blank background pane. */
    private void fillBackground(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, filler);
        }
    }

    private boolean hasWorldEditSelection(Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Type Replace Tool").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        meta.lore(Arrays.asList(
                Component.text("Replace all variants of one").color(NamedTextColor.GRAY),
                Component.text("material with another.").color(NamedTextColor.GRAY),
                Component.text(""),
                Component.text("Converts: stairs, slabs,").color(NamedTextColor.YELLOW),
                Component.text("walls, fences, bars, etc.").color(NamedTextColor.YELLOW)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSelectionStatusItem(Player player) {
        boolean hasSelection = hasWorldEditSelection(player);
        ItemStack item = new ItemStack(hasSelection ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(hasSelection ? "Selection Ready" : "No Selection!")
                .color(hasSelection ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (!hasSelection) {
            meta.lore(Arrays.asList(
                    Component.text("Use WorldEdit to select").color(NamedTextColor.GRAY),
                    Component.text("an area first (//wand)").color(NamedTextColor.GRAY)));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaterialDisplayItem(String materialName, String label) {
        ItemStack item = new ItemStack(getMaterialIcon(materialName));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label + ": " + formatMaterialName(materialName))
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaterialItem(String materialName) {
        ItemStack item = new ItemStack(getMaterialIcon(materialName));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(formatMaterialName(materialName)).color(NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialIcon(String materialName) {
        BlockFamily family = blockFamilies.family(materialName);
        BlockType baseBlock = family.getVariant("block");
        if (baseBlock != null) {
            String id = baseBlock.id().replace("minecraft:", "").toUpperCase();
            Material mat = Material.matchMaterial(id);
            if (mat != null)
                return mat;
        }

        Material direct = Material.matchMaterial(materialName.toUpperCase());
        if (direct != null)
            return direct;

        Material planks = Material.matchMaterial(materialName.toUpperCase() + "_PLANKS");
        if (planks != null)
            return planks;

        Material block = Material.matchMaterial(materialName.toUpperCase() + "_BLOCK");
        if (block != null)
            return block;

        return Material.STONE;
    }

    private String formatMaterialName(String materialName) {
        return Arrays.stream(materialName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(materialName);
    }
}
