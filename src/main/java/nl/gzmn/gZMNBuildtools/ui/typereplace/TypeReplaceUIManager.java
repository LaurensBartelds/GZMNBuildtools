package nl.gzmn.gZMNBuildtools.ui.typereplace;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.typereplace.BlockTypeFamily;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class TypeReplaceUIManager implements Listener {

    private final Plugin plugin;
    private final TypeReplaceCommand typeReplaceCommand;
    private final Map<UUID, TypeReplaceBuilder> activeBuilders;

    private final NamespacedKey selectingKey;
    private final NamespacedKey categoryKey;

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

    private static final Map<MaterialCategory, List<String>> CATEGORY_MATERIALS = new EnumMap<>(MaterialCategory.class);

    static {
        CATEGORY_MATERIALS.put(MaterialCategory.STONE, Arrays.asList(
                "stone", "cobblestone", "mossy_cobblestone",
                "stone_brick", "mossy_stone_brick",
                "granite", "polished_granite",
                "diorite", "polished_diorite",
                "andesite", "polished_andesite"));

        CATEGORY_MATERIALS.put(MaterialCategory.DEEPSLATE, Arrays.asList(
                "deepslate", "cobbled_deepslate", "polished_deepslate",
                "deepslate_brick", "deepslate_tile"));

        CATEGORY_MATERIALS.put(MaterialCategory.BRICKS, Arrays.asList(
                "brick", "mud_brick"));

        CATEGORY_MATERIALS.put(MaterialCategory.SANDSTONE, Arrays.asList(
                "sandstone", "smooth_sandstone", "cut_sandstone",
                "red_sandstone", "smooth_red_sandstone", "cut_red_sandstone"));

        CATEGORY_MATERIALS.put(MaterialCategory.WOOD, Arrays.asList(
                "oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "mangrove", "cherry", "bamboo",
                "crimson", "warped"));

        CATEGORY_MATERIALS.put(MaterialCategory.NETHER, Arrays.asList(
                "nether_brick", "red_nether_brick",
                "blackstone", "polished_blackstone", "polished_blackstone_brick"));

        CATEGORY_MATERIALS.put(MaterialCategory.COPPER, Arrays.asList(
                "copper", "cut_copper",
                "exposed_copper", "exposed_cut_copper",
                "weathered_copper", "weathered_cut_copper",
                "oxidized_copper", "oxidized_cut_copper",
                "waxed_copper", "waxed_cut_copper",
                "waxed_exposed_copper", "waxed_exposed_cut_copper",
                "waxed_weathered_copper", "waxed_weathered_cut_copper",
                "waxed_oxidized_copper", "waxed_oxidized_cut_copper"));

        CATEGORY_MATERIALS.put(MaterialCategory.PRISMARINE, Arrays.asList(
                "prismarine", "prismarine_brick", "dark_prismarine"));

        CATEGORY_MATERIALS.put(MaterialCategory.QUARTZ, Arrays.asList(
                "quartz", "smooth_quartz"));

        CATEGORY_MATERIALS.put(MaterialCategory.END, Arrays.asList(
                "end_stone_brick", "purpur"));

        CATEGORY_MATERIALS.put(MaterialCategory.TUFF, Arrays.asList(
                "tuff", "polished_tuff", "tuff_brick"));

        CATEGORY_MATERIALS.put(MaterialCategory.OTHER, Arrays.asList(
                "iron"));
    }

    public TypeReplaceUIManager(Plugin plugin, TypeReplaceCommand typeReplaceCommand) {
        this.plugin = plugin;
        this.typeReplaceCommand = typeReplaceCommand;
        this.activeBuilders = new HashMap<>();
        this.selectingKey = new NamespacedKey(plugin, "typereplace_selecting");
        this.categoryKey = new NamespacedKey(plugin, "typereplace_category");
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTypeReplaceUI(Player player) {
        TypeReplaceBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(),
                k -> new TypeReplaceBuilder());

        Inventory inventory = createMainInventory(player, builder);
        player.openInventory(inventory);
    }

    private Inventory createMainInventory(Player player, TypeReplaceBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Type Replace").color(NamedTextColor.DARK_AQUA)
                        .decorate(TextDecoration.BOLD));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

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

    private void openCategorySelector(Player player, String selecting) {
        player.getPersistentDataContainer().set(selectingKey, PersistentDataType.STRING, selecting);

        String title = selecting.equals("source") ? "Select Source Category" : "Select Target Category";
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(title).color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.BOLD));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

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
                    Component.text(CATEGORY_MATERIALS.get(category).size() + " materials")
                            .color(NamedTextColor.GRAY)));
            item.setItemMeta(meta);
            inv.setItem(9 + i, item);
        }

        player.openInventory(inv);
    }

    private void openMaterialSelector(Player player, MaterialCategory category) {
        player.getPersistentDataContainer().set(categoryKey, PersistentDataType.INTEGER, category.ordinal());

        String selecting = player.getPersistentDataContainer().get(selectingKey, PersistentDataType.STRING);
        String title = selecting != null && selecting.equals("source") ? "Select Source Material"
                : "Select Target Material";

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(title).color(NamedTextColor.DARK_GREEN)
                        .decorate(TextDecoration.BOLD));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

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

        List<String> materials = CATEGORY_MATERIALS.get(category);
        for (int i = 0; i < Math.min(materials.size(), 36); i++) {
            String materialName = materials.get(i);
            ItemStack item = createMaterialItem(materialName);
            inv.setItem(9 + i, item);
        }

        inv.setItem(53, back.clone());

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains("Type Replace") && !title.contains("Category") && !title.contains("Material")) {
            handleMainPageClick(event, player);
        } else if (title.contains("Category")) {
            handleCategoryPageClick(event, player);
        } else if (title.contains("Material")) {
            handleMaterialPageClick(event, player);
        }
    }

    private void handleMainPageClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        TypeReplaceBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        int slot = event.getSlot();

        switch (slot) {
            case 37 -> openCategorySelector(player, "source");
            case 43 -> openCategorySelector(player, "target");
            case 49 -> executeReplacement(player, builder);
            case 53 -> player.closeInventory();
        }
    }

    private void handleCategoryPageClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();

        if (slot == 0) {
            openTypeReplaceUI(player);
            return;
        }

        if (slot >= 9 && slot <= 20) {
            int categoryIndex = slot - 9;
            MaterialCategory[] categories = MaterialCategory.values();
            if (categoryIndex < categories.length) {
                openMaterialSelector(player, categories[categoryIndex]);
            }
        }
    }

    private void handleMaterialPageClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();

        if (slot == 0 || slot == 53) {
            String selecting = player.getPersistentDataContainer().get(selectingKey, PersistentDataType.STRING);
            if (selecting != null) {
                openCategorySelector(player, selecting);
            } else {
                openTypeReplaceUI(player);
            }
            return;
        }

        if (slot >= 9 && slot <= 44) {
            Integer categoryOrdinal = player.getPersistentDataContainer().get(categoryKey, PersistentDataType.INTEGER);
            if (categoryOrdinal == null)
                return;

            MaterialCategory category = MaterialCategory.values()[categoryOrdinal];
            List<String> materials = CATEGORY_MATERIALS.get(category);

            int materialIndex = slot - 9;
            if (materialIndex < materials.size()) {
                String selectedMaterial = materials.get(materialIndex);
                setMaterialSelection(player, selectedMaterial);
                openTypeReplaceUI(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains("Type Replace") && !title.contains("Category") && !title.contains("Material")) {
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    activeBuilders.remove(player.getUniqueId());
                    cleanupPDC(player);
                }
            }, 6000L);
        }
    }

    private void setMaterialSelection(Player player, String material) {
        TypeReplaceBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        String selecting = player.getPersistentDataContainer().get(selectingKey, PersistentDataType.STRING);
        if (selecting == null)
            return;

        if (selecting.equals("source")) {
            builder.sourceMaterial = material;
        } else {
            builder.targetMaterial = material;
        }
    }

    private void executeReplacement(Player player, TypeReplaceBuilder builder) {
        if (builder.sourceMaterial == null) {
            MessageManager.error(player, "Please select a source material.");
            return;
        }
        if (builder.targetMaterial == null) {
            MessageManager.error(player, "Please select a target material.");
            return;
        }

        player.closeInventory();

        typeReplaceCommand.execute(player, builder.sourceMaterial, builder.targetMaterial);

        activeBuilders.remove(player.getUniqueId());
        cleanupPDC(player);
    }

    private void cleanupPDC(Player player) {
        player.getPersistentDataContainer().remove(selectingKey);
        player.getPersistentDataContainer().remove(categoryKey);
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
        Material icon = getMaterialIcon(materialName);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label + ": " + formatMaterialName(materialName))
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaterialItem(String materialName) {
        Material icon = getMaterialIcon(materialName);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(formatMaterialName(materialName)).color(NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialIcon(String materialName) {
        BlockTypeFamily family = new BlockTypeFamily(materialName);
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

    private static class TypeReplaceBuilder {
        String sourceMaterial;
        String targetMaterial;
    }
}
