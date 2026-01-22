package nl.gzmn.gZMNBuildtools.ui;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Category-based block browser for gradient stop selection.
 * Supports single block and multi-block selection modes.
 * Replaces chat-based input with intuitive UI.
 */
public class GradientBlockBrowser implements Listener {

    private final Plugin plugin;
    private final NamespacedKey stopIndexKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey pageKey;
    private final NamespacedKey multiSelectKey;

    // Callbacks for when block selection is complete
    private final Map<UUID, BiConsumer<Integer, List<BlockType>>> selectionCallbacks = new HashMap<>();

    // Multi-block selection state
    private final Map<UUID, List<BlockType>> pendingMultiSelect = new HashMap<>();

    /**
     * Block categories optimized for gradient creation.
     */
    public enum BlockCategory {
        CONCRETE("Concrete", Material.WHITE_CONCRETE, "16 solid colors"),
        WOOL("Wool", Material.WHITE_WOOL, "16 soft colors"),
        TERRACOTTA("Terracotta", Material.TERRACOTTA, "17 earthy tones"),
        GLAZED_TERRACOTTA("Glazed Terracotta", Material.WHITE_GLAZED_TERRACOTTA, "16 decorative"),
        STONE("Stone Types", Material.STONE, "Natural stone"),
        DEEPSLATE("Deepslate", Material.DEEPSLATE, "Deep underground"),
        WOOD("Wood Planks", Material.OAK_PLANKS, "11 wood types"),
        NATURAL("Natural", Material.GRASS_BLOCK, "Dirt, grass, sand"),
        NETHER("Nether", Material.NETHERRACK, "Nether blocks"),
        END("End & Prismarine", Material.END_STONE, "End & ocean"),
        ICE("Ice & Snow", Material.ICE, "Frozen blocks"),
        GLASS("Glass", Material.GLASS, "Transparent colors");

        final String displayName;
        final Material icon;
        final String description;

        BlockCategory(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }

    private static final Map<BlockCategory, List<String>> CATEGORY_BLOCKS = new EnumMap<>(BlockCategory.class);

    static {
        // Concrete - ordered for gradients (light to dark)
        CATEGORY_BLOCKS.put(BlockCategory.CONCRETE, Arrays.asList(
                "white_concrete", "light_gray_concrete", "gray_concrete", "black_concrete",
                "yellow_concrete", "orange_concrete", "red_concrete", "brown_concrete",
                "lime_concrete", "green_concrete", "cyan_concrete", "light_blue_concrete",
                "blue_concrete", "purple_concrete", "magenta_concrete", "pink_concrete"
        ));

        // Wool - same order as concrete
        CATEGORY_BLOCKS.put(BlockCategory.WOOL, Arrays.asList(
                "white_wool", "light_gray_wool", "gray_wool", "black_wool",
                "yellow_wool", "orange_wool", "red_wool", "brown_wool",
                "lime_wool", "green_wool", "cyan_wool", "light_blue_wool",
                "blue_wool", "purple_wool", "magenta_wool", "pink_wool"
        ));

        // Terracotta - includes uncolored
        CATEGORY_BLOCKS.put(BlockCategory.TERRACOTTA, Arrays.asList(
                "terracotta", "white_terracotta", "light_gray_terracotta", "gray_terracotta",
                "black_terracotta", "yellow_terracotta", "orange_terracotta", "red_terracotta",
                "brown_terracotta", "lime_terracotta", "green_terracotta", "cyan_terracotta",
                "light_blue_terracotta", "blue_terracotta", "purple_terracotta", "magenta_terracotta",
                "pink_terracotta"
        ));

        // Glazed Terracotta
        CATEGORY_BLOCKS.put(BlockCategory.GLAZED_TERRACOTTA, Arrays.asList(
                "white_glazed_terracotta", "light_gray_glazed_terracotta", "gray_glazed_terracotta",
                "black_glazed_terracotta", "yellow_glazed_terracotta", "orange_glazed_terracotta",
                "red_glazed_terracotta", "brown_glazed_terracotta", "lime_glazed_terracotta",
                "green_glazed_terracotta", "cyan_glazed_terracotta", "light_blue_glazed_terracotta",
                "blue_glazed_terracotta", "purple_glazed_terracotta", "magenta_glazed_terracotta",
                "pink_glazed_terracotta"
        ));

        // Stone types - gradient friendly order
        CATEGORY_BLOCKS.put(BlockCategory.STONE, Arrays.asList(
                "stone", "cobblestone", "mossy_cobblestone", "stone_bricks",
                "cracked_stone_bricks", "mossy_stone_bricks", "andesite", "polished_andesite",
                "diorite", "polished_diorite", "granite", "polished_granite",
                "calcite", "tuff", "polished_tuff", "tuff_bricks",
                "dripstone_block", "smooth_stone", "bricks", "mud_bricks"
        ));

        // Deepslate
        CATEGORY_BLOCKS.put(BlockCategory.DEEPSLATE, Arrays.asList(
                "deepslate", "cobbled_deepslate", "polished_deepslate",
                "deepslate_bricks", "cracked_deepslate_bricks",
                "deepslate_tiles", "cracked_deepslate_tiles",
                "chiseled_deepslate", "reinforced_deepslate"
        ));

        // Wood planks
        CATEGORY_BLOCKS.put(BlockCategory.WOOD, Arrays.asList(
                "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
                "acacia_planks", "dark_oak_planks", "mangrove_planks", "cherry_planks",
                "bamboo_planks", "crimson_planks", "warped_planks"
        ));

        // Natural blocks
        CATEGORY_BLOCKS.put(BlockCategory.NATURAL, Arrays.asList(
                "grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol",
                "mycelium", "mud", "packed_mud", "gravel", "sand", "red_sand",
                "sandstone", "red_sandstone", "clay", "moss_block"
        ));

        // Nether blocks
        CATEGORY_BLOCKS.put(BlockCategory.NETHER, Arrays.asList(
                "netherrack", "nether_bricks", "red_nether_bricks", "cracked_nether_bricks",
                "chiseled_nether_bricks", "basalt", "polished_basalt", "smooth_basalt",
                "blackstone", "polished_blackstone", "polished_blackstone_bricks",
                "gilded_blackstone", "soul_sand", "soul_soil", "magma_block",
                "glowstone", "shroomlight", "crying_obsidian", "obsidian"
        ));

        // End & Prismarine
        CATEGORY_BLOCKS.put(BlockCategory.END, Arrays.asList(
                "end_stone", "end_stone_bricks", "purpur_block", "purpur_pillar",
                "prismarine", "prismarine_bricks", "dark_prismarine", "sea_lantern",
                "quartz_block", "smooth_quartz", "quartz_bricks", "chiseled_quartz_block"
        ));

        // Ice & Snow
        CATEGORY_BLOCKS.put(BlockCategory.ICE, Arrays.asList(
                "snow_block", "powder_snow", "ice", "packed_ice", "blue_ice",
                "frosted_ice"
        ));

        // Glass
        CATEGORY_BLOCKS.put(BlockCategory.GLASS, Arrays.asList(
                "glass", "white_stained_glass", "light_gray_stained_glass", "gray_stained_glass",
                "black_stained_glass", "yellow_stained_glass", "orange_stained_glass", "red_stained_glass",
                "brown_stained_glass", "lime_stained_glass", "green_stained_glass", "cyan_stained_glass",
                "light_blue_stained_glass", "blue_stained_glass", "purple_stained_glass", "magenta_stained_glass",
                "pink_stained_glass", "tinted_glass"
        ));
    }

    public GradientBlockBrowser(Plugin plugin) {
        this.plugin = plugin;
        this.stopIndexKey = new NamespacedKey(plugin, "gradient_stop_index");
        this.categoryKey = new NamespacedKey(plugin, "gradient_category");
        this.pageKey = new NamespacedKey(plugin, "gradient_page");
        this.multiSelectKey = new NamespacedKey(plugin, "gradient_multi_select");
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the block browser for selecting a single block.
     *
     * @param player The player
     * @param stopIndex The gradient stop index being edited
     * @param callback Called when selection is complete (stopIndex, list with single block)
     */
    public void openSingleSelect(Player player, int stopIndex, BiConsumer<Integer, List<BlockType>> callback) {
        selectionCallbacks.put(player.getUniqueId(), callback);
        player.getPersistentDataContainer().set(stopIndexKey, PersistentDataType.INTEGER, stopIndex);
        player.getPersistentDataContainer().set(multiSelectKey, PersistentDataType.BYTE, (byte) 0);
        openCategorySelector(player);
    }

    /**
     * Open the block browser for selecting multiple blocks (randomization).
     *
     * @param player The player
     * @param stopIndex The gradient stop index being edited
     * @param callback Called when selection is complete (stopIndex, list of blocks)
     */
    public void openMultiSelect(Player player, int stopIndex, BiConsumer<Integer, List<BlockType>> callback) {
        selectionCallbacks.put(player.getUniqueId(), callback);
        pendingMultiSelect.put(player.getUniqueId(), new ArrayList<>());
        player.getPersistentDataContainer().set(stopIndexKey, PersistentDataType.INTEGER, stopIndex);
        player.getPersistentDataContainer().set(multiSelectKey, PersistentDataType.BYTE, (byte) 1);
        openCategorySelector(player);
    }

    private void openCategorySelector(Player player) {
        boolean isMultiSelect = isMultiSelectMode(player);
        String title = isMultiSelect ? "Select Blocks (Multi)" : "Select Block";

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Gradient: " + title).color(NamedTextColor.DARK_GREEN)
                        .decorate(TextDecoration.BOLD));

        fillBackground(inv);

        // Back button
        inv.setItem(0, createBackButton("← Cancel"));

        // Title/info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Select Block Category").color(NamedTextColor.GOLD));
        if (isMultiSelect) {
            List<BlockType> pending = pendingMultiSelect.getOrDefault(player.getUniqueId(), new ArrayList<>());
            infoMeta.lore(Arrays.asList(
                    Component.text("Multi-select mode").color(NamedTextColor.AQUA),
                    Component.text("Selected: " + pending.size() + " blocks").color(NamedTextColor.GRAY),
                    Component.text(""),
                    Component.text("Click blocks to add").color(NamedTextColor.YELLOW),
                    Component.text("Shift+click 'Done' when finished").color(NamedTextColor.YELLOW)
            ));
        } else {
            infoMeta.lore(Arrays.asList(
                    Component.text("Choose a category, then").color(NamedTextColor.GRAY),
                    Component.text("select a block").color(NamedTextColor.GRAY)
            ));
        }
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Categories - 2 rows of 6
        BlockCategory[] categories = BlockCategory.values();
        int[] categorySlots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};
        for (int i = 0; i < Math.min(categories.length, categorySlots.length); i++) {
            BlockCategory category = categories[i];
            ItemStack item = new ItemStack(category.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(category.displayName).color(NamedTextColor.YELLOW));
            meta.lore(Arrays.asList(
                    Component.text(category.description).color(NamedTextColor.GRAY),
                    Component.text(CATEGORY_BLOCKS.get(category).size() + " blocks").color(NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
            inv.setItem(categorySlots[i], item);
        }

        // Multi-select: Done button
        if (isMultiSelect) {
            List<BlockType> pending = pendingMultiSelect.getOrDefault(player.getUniqueId(), new ArrayList<>());
            ItemStack done = new ItemStack(pending.isEmpty() ? Material.GRAY_DYE : Material.LIME_DYE);
            ItemMeta doneMeta = done.getItemMeta();
            doneMeta.displayName(Component.text("Done").color(pending.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            if (!pending.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Selected blocks:").color(NamedTextColor.GRAY));
                for (BlockType bt : pending) {
                    lore.add(Component.text("  • " + formatBlockName(bt.id())).color(NamedTextColor.AQUA));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("Click to confirm selection").color(NamedTextColor.YELLOW));
                doneMeta.lore(lore);
            } else {
                doneMeta.lore(Arrays.asList(
                        Component.text("Select at least one block").color(NamedTextColor.RED)
                ));
            }
            done.setItemMeta(doneMeta);
            inv.setItem(49, done);
        }

        player.openInventory(inv);
    }

    private void openBlockSelector(Player player, BlockCategory category, int page) {
        player.getPersistentDataContainer().set(categoryKey, PersistentDataType.INTEGER, category.ordinal());
        player.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);

        List<String> blocks = CATEGORY_BLOCKS.get(category);
        int totalPages = (int) Math.ceil(blocks.size() / 28.0); // 28 blocks per page (4 rows of 7)
        int startIndex = page * 28;

        boolean isMultiSelect = isMultiSelectMode(player);
        String title = category.displayName + " (" + (page + 1) + "/" + Math.max(1, totalPages) + ")";

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Gradient: " + title).color(NamedTextColor.DARK_GREEN)
                        .decorate(TextDecoration.BOLD));

        fillBackground(inv);

        // Back button
        inv.setItem(0, createBackButton("← Categories"));

        // Category info
        ItemStack categoryInfo = new ItemStack(category.icon);
        ItemMeta catMeta = categoryInfo.getItemMeta();
        catMeta.displayName(Component.text(category.displayName).color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        catMeta.lore(Arrays.asList(
                Component.text(category.description).color(NamedTextColor.GRAY)
        ));
        categoryInfo.setItemMeta(catMeta);
        inv.setItem(4, categoryInfo);

        // Multi-select status
        if (isMultiSelect) {
            List<BlockType> pending = pendingMultiSelect.getOrDefault(player.getUniqueId(), new ArrayList<>());
            ItemStack status = new ItemStack(Material.CHEST);
            ItemMeta statusMeta = status.getItemMeta();
            statusMeta.displayName(Component.text("Selected: " + pending.size()).color(NamedTextColor.AQUA));
            if (!pending.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (BlockType bt : pending) {
                    lore.add(Component.text("• " + formatBlockName(bt.id())).color(NamedTextColor.GRAY));
                }
                statusMeta.lore(lore);
            }
            status.setItemMeta(statusMeta);
            inv.setItem(8, status);
        }

        // Blocks - slots 10-16, 19-25, 28-34, 37-43 (4 rows of 7)
        int[] blockSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        List<BlockType> pendingBlocks = pendingMultiSelect.getOrDefault(player.getUniqueId(), new ArrayList<>());

        for (int i = 0; i < blockSlots.length && (startIndex + i) < blocks.size(); i++) {
            String blockId = blocks.get(startIndex + i);
            BlockType blockType = BlockTypes.get("minecraft:" + blockId);

            ItemStack item = createBlockItem(blockId, blockType);

            // Mark if already selected in multi-select mode
            if (isMultiSelect && blockType != null && pendingBlocks.contains(blockType)) {
                ItemMeta meta = item.getItemMeta();
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text("✓ SELECTED").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                meta.lore(lore);
                item.setItemMeta(meta);
                item.setType(Material.LIME_STAINED_GLASS_PANE);
            }

            inv.setItem(blockSlots[i], item);
        }

        // Pagination
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("← Previous Page").color(NamedTextColor.YELLOW));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next Page →").color(NamedTextColor.YELLOW));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        // Done button for multi-select
        if (isMultiSelect) {
            ItemStack done = new ItemStack(pendingBlocks.isEmpty() ? Material.GRAY_DYE : Material.LIME_DYE);
            ItemMeta doneMeta = done.getItemMeta();
            doneMeta.displayName(Component.text("Done").color(pendingBlocks.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            done.setItemMeta(doneMeta);
            inv.setItem(49, done);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!title.startsWith("Gradient:")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getSlot();

        if (title.contains("Select Block") || title.contains("Multi")) {
            handleCategoryClick(event, player, slot);
        } else {
            handleBlockClick(event, player, slot);
        }
    }

    private void handleCategoryClick(InventoryClickEvent event, Player player, int slot) {
        // Back/Cancel
        if (slot == 0) {
            cancelSelection(player);
            return;
        }

        // Done button (multi-select)
        if (slot == 49 && isMultiSelectMode(player)) {
            completeMultiSelect(player);
            return;
        }

        // Category slots
        int[] categorySlots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};
        BlockCategory[] categories = BlockCategory.values();
        for (int i = 0; i < categorySlots.length; i++) {
            if (slot == categorySlots[i] && i < categories.length) {
                openBlockSelector(player, categories[i], 0);
                return;
            }
        }
    }

    private void handleBlockClick(InventoryClickEvent event, Player player, int slot) {
        // Back to categories
        if (slot == 0) {
            openCategorySelector(player);
            return;
        }

        // Done button (multi-select)
        if (slot == 49 && isMultiSelectMode(player)) {
            completeMultiSelect(player);
            return;
        }

        // Pagination
        Integer categoryOrdinal = player.getPersistentDataContainer().get(categoryKey, PersistentDataType.INTEGER);
        Integer page = player.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
        if (categoryOrdinal == null || page == null) return;

        BlockCategory category = BlockCategory.values()[categoryOrdinal];

        if (slot == 45 && page > 0) {
            openBlockSelector(player, category, page - 1);
            return;
        }

        List<String> blocks = CATEGORY_BLOCKS.get(category);
        int totalPages = (int) Math.ceil(blocks.size() / 28.0);
        if (slot == 53 && page < totalPages - 1) {
            openBlockSelector(player, category, page + 1);
            return;
        }

        // Block selection
        int[] blockSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < blockSlots.length; i++) {
            if (slot == blockSlots[i]) {
                int blockIndex = page * 28 + i;
                if (blockIndex < blocks.size()) {
                    String blockId = blocks.get(blockIndex);
                    BlockType blockType = BlockTypes.get("minecraft:" + blockId);
                    if (blockType != null) {
                        handleBlockSelection(player, blockType, event.getClick());
                    }
                }
                return;
            }
        }
    }

    private void handleBlockSelection(Player player, BlockType blockType, ClickType clickType) {
        if (isMultiSelectMode(player)) {
            // Toggle selection in multi-select mode
            List<BlockType> pending = pendingMultiSelect.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
            if (pending.contains(blockType)) {
                pending.remove(blockType);
            } else {
                pending.add(blockType);
            }

            // Refresh the current page
            Integer categoryOrdinal = player.getPersistentDataContainer().get(categoryKey, PersistentDataType.INTEGER);
            Integer page = player.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (categoryOrdinal != null && page != null) {
                openBlockSelector(player, BlockCategory.values()[categoryOrdinal], page);
            }
        } else {
            // Single select - complete immediately
            completeSingleSelect(player, blockType);
        }
    }

    private void completeSingleSelect(Player player, BlockType blockType) {
        Integer stopIndex = player.getPersistentDataContainer().get(stopIndexKey, PersistentDataType.INTEGER);
        BiConsumer<Integer, List<BlockType>> callback = selectionCallbacks.remove(player.getUniqueId());

        cleanup(player);
        player.closeInventory();

        if (callback != null && stopIndex != null) {
            callback.accept(stopIndex, Arrays.asList(blockType));
        }
    }

    private void completeMultiSelect(Player player) {
        List<BlockType> selected = pendingMultiSelect.remove(player.getUniqueId());
        if (selected == null || selected.isEmpty()) return;

        Integer stopIndex = player.getPersistentDataContainer().get(stopIndexKey, PersistentDataType.INTEGER);
        BiConsumer<Integer, List<BlockType>> callback = selectionCallbacks.remove(player.getUniqueId());

        cleanup(player);
        player.closeInventory();

        if (callback != null && stopIndex != null) {
            callback.accept(stopIndex, selected);
        }
    }

    private void cancelSelection(Player player) {
        selectionCallbacks.remove(player.getUniqueId());
        pendingMultiSelect.remove(player.getUniqueId());
        cleanup(player);
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Gradient:")) return;

        // Delayed cleanup to handle reopening
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String newTitle = PlainTextComponentSerializer.plainText()
                    .serialize(player.getOpenInventory().title());
            if (!newTitle.startsWith("Gradient:")) {
                selectionCallbacks.remove(player.getUniqueId());
                pendingMultiSelect.remove(player.getUniqueId());
                cleanup(player);
            }
        }, 2L);
    }

    private void cleanup(Player player) {
        player.getPersistentDataContainer().remove(stopIndexKey);
        player.getPersistentDataContainer().remove(categoryKey);
        player.getPersistentDataContainer().remove(pageKey);
        player.getPersistentDataContainer().remove(multiSelectKey);
    }

    private boolean isMultiSelectMode(Player player) {
        Byte value = player.getPersistentDataContainer().get(multiSelectKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private void fillBackground(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack createBackButton(String text) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(text).color(NamedTextColor.GRAY));
        back.setItemMeta(meta);
        return back;
    }

    private ItemStack createBlockItem(String blockId, BlockType blockType) {
        Material material = Material.matchMaterial(blockId.toUpperCase());
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(formatBlockName(blockId)).color(NamedTextColor.YELLOW));

        if (blockType == null) {
            meta.lore(Arrays.asList(
                    Component.text("Block not found").color(NamedTextColor.RED)
            ));
        }

        item.setItemMeta(meta);
        return item;
    }

    private String formatBlockName(String blockId) {
        String name = blockId.replace("minecraft:", "");
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(name);
    }

    /**
     * Get all blocks in a category.
     */
    public static List<String> getBlocksInCategory(BlockCategory category) {
        return new ArrayList<>(CATEGORY_BLOCKS.get(category));
    }
}
