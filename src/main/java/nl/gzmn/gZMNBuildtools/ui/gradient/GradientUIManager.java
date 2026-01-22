package nl.gzmn.gZMNBuildtools.ui.gradient;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.command.GradientCommand;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition.GradientStop;
import nl.gzmn.gZMNBuildtools.gradient.model.GradientDefinition.InterpolationMode;
import nl.gzmn.gZMNBuildtools.gradient.service.BlockColorService;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;


public class GradientUIManager implements Listener {

    private static final String MODE_SELECTION_TITLE = "Gradient Mode";
    private static final String LEGACY_BUILDER_TITLE = "Gradient Builder";
    private static final String LEGACY_BLOCK_SELECTOR_TITLE = "Select Block";

    private final Plugin plugin;
    private final GradientCommand gradientCommand;
    private final Map<UUID, GradientBuilder> activeBuilders;
    private GradientStorageManager storageManager;

    private GradientPreviewRenderer previewRenderer;
    private GradientEasyModeUI easyModeUI;
    private GradientAdvancedModeUI advancedModeUI;

    public GradientUIManager(Plugin plugin, GradientCommand gradientCommand) {
        this.plugin = plugin;
        this.gradientCommand = gradientCommand;
        this.activeBuilders = new HashMap<>();
    }

    public void setStorageManager(GradientStorageManager storageManager) {
        this.storageManager = storageManager;
        if (easyModeUI != null) {
            easyModeUI.setStorageManager(storageManager);
        }
        if (advancedModeUI != null) {
            advancedModeUI.setStorageManager(storageManager);
        }
    }

    public void setBlockColorService(BlockColorService blockColorService) {
        if (advancedModeUI != null) {
            advancedModeUI.setBlockColorService(blockColorService);
        }
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.previewRenderer = new GradientPreviewRenderer(plugin);
        this.easyModeUI = new GradientEasyModeUI(plugin, previewRenderer);
        this.advancedModeUI = new GradientAdvancedModeUI(plugin, previewRenderer);

        easyModeUI.registerEvents();
        advancedModeUI.registerEvents();
    }

    
    public void openModeSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(MODE_SELECTION_TITLE).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(
                Component.text("Gradient Builder").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        infoMeta.lore(Arrays.asList(
                Component.text("Choose your workflow:").color(NamedTextColor.GRAY)));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        ItemStack easyMode = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta easyMeta = easyMode.getItemMeta();
        easyMeta.displayName(Component.text("Easy Mode").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        easyMeta.lore(Arrays.asList(
                Component.text("Quick presets").color(NamedTextColor.GRAY),
                Component.text("Simple 2-click workflow").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Best for: Quick gradients").color(NamedTextColor.DARK_GRAY)));
        easyMode.setItemMeta(easyMeta);
        inv.setItem(11, easyMode);

        ItemStack advancedMode = new ItemStack(Material.DIAMOND);
        ItemMeta advancedMeta = advancedMode.getItemMeta();
        advancedMeta.displayName(
                Component.text("Advanced Mode").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        advancedMeta.lore(Arrays.asList(
                Component.text("Full control").color(NamedTextColor.GRAY),
                Component.text("Custom stops & noise").color(NamedTextColor.GRAY),
                Component.text("World preview").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Best for: Complex gradients").color(NamedTextColor.DARK_GRAY)));
        advancedMode.setItemMeta(advancedMeta);
        inv.setItem(15, advancedMode);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inv.setItem(26, close);

        player.openInventory(inv);
    }

    
    public void openEasyMode(Player player) {
        easyModeUI.openPresetGallery(player);
    }

    
    public void openAdvancedMode(Player player) {
        advancedModeUI.openMainUI(player);
    }

    
    
    public void openGradientUI(Player player) {
        openEasyMode(player);
    }

    
    public GradientPreviewRenderer getPreviewRenderer() {
        return previewRenderer;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains(MODE_SELECTION_TITLE)) {
            handleModeSelectionClick(event, player);
        } else if (title.contains(LEGACY_BUILDER_TITLE) && !title.contains("Advanced") && !title.contains("Presets")) {
            handleLegacyBuilderClick(event, player);
        } else if (title.equals(LEGACY_BLOCK_SELECTOR_TITLE)) {
            handleLegacyBlockSelectorClick(event, player);
        }
    }

    private void handleModeSelectionClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();

        if (slot == 11) {
            openEasyMode(player);
        } else if (slot == 15) {
            openAdvancedMode(player);
        } else if (slot == 26) {
            player.closeInventory();
        }
    }

    private void handleLegacyBuilderClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        GradientBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        int slot = event.getSlot();

        if (slot >= 0 && slot < 9) {
            if (event.getCurrentItem().getType() == Material.LIME_DYE) {
                player.closeInventory();
                openLegacyBlockSelector(player, builder.stops.size());
            } else if (event.isRightClick() && slot < builder.stops.size()) {
                builder.stops.remove(slot);
                player.openInventory(createLegacyGradientInventory(builder));
            }
        } else if (slot >= 18 && slot <= 22) {
            GradientDirection[] directions = GradientDirection.values();
            int dirIndex = slot - 18;
            if (dirIndex < directions.length) {
                builder.direction = directions[dirIndex];
                player.openInventory(createLegacyGradientInventory(builder));
            }
        } else if (slot >= 27 && slot <= 29) {
            InterpolationMode[] modes = InterpolationMode.values();
            int modeIndex = slot - 27;
            if (modeIndex < modes.length) {
                builder.interpolationMode = modes[modeIndex];
                player.openInventory(createLegacyGradientInventory(builder));
            }
        } else if (slot == 49) {
            applyLegacyGradient(player, builder);
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    private void handleLegacyBlockSelectorClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        GradientBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        int slot = event.getSlot();

        if (slot == 53) {
            player.openInventory(createLegacyGradientInventory(builder));
            return;
        }

        if (slot < 45) {
            Material selectedMaterial = event.getCurrentItem().getType();
            BlockType blockType = BlockTypes.get("minecraft:" + selectedMaterial.name().toLowerCase());

            if (blockType != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "editing_stop");
                Integer stopIndex = player.getPersistentDataContainer().get(key,
                        org.bukkit.persistence.PersistentDataType.INTEGER);

                if (stopIndex != null) {
                    if (stopIndex < builder.stops.size()) {
                        builder.stops.set(stopIndex, blockType);
                    } else {
                        builder.stops.add(blockType);
                    }
                    player.getPersistentDataContainer().remove(key);
                }
            }

            player.openInventory(createLegacyGradientInventory(builder));
        }
    }

    private Inventory createLegacyGradientInventory(GradientBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(LEGACY_BUILDER_TITLE).color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD));

        for (int i = 0; i < 9; i++) {
            if (i < builder.stops.size()) {
                BlockType blockType = builder.stops.get(i);
                Material material = Material.matchMaterial(blockType.id().replace("minecraft:", "").toUpperCase());
                if (material != null) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.displayName(
                            Component.text("Stop " + (i + 1) + ": " + blockType.id()).color(NamedTextColor.YELLOW));
                    meta.lore(Arrays.asList(
                            Component.text("Left-click to change").color(NamedTextColor.GRAY),
                            Component.text("Right-click to remove").color(NamedTextColor.GRAY)));
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }
            } else {
                ItemStack addStop = new ItemStack(Material.LIME_DYE);
                ItemMeta meta = addStop.getItemMeta();
                meta.displayName(Component.text("Add Gradient Stop").color(NamedTextColor.GREEN));
                meta.lore(Arrays.asList(
                        Component.text("Click to add a block").color(NamedTextColor.GRAY)));
                addStop.setItemMeta(meta);
                inv.setItem(i, addStop);
                break;
            }
        }

        addDirectionButton(inv, 18, GradientDirection.VERTICAL_UP, "Vertical Up", Material.ARROW, builder);
        addDirectionButton(inv, 19, GradientDirection.VERTICAL_DOWN, "Vertical Down", Material.ARROW, builder);
        addDirectionButton(inv, 20, GradientDirection.HORIZONTAL_X, "Horizontal X", Material.ARROW, builder);
        addDirectionButton(inv, 21, GradientDirection.HORIZONTAL_Z, "Horizontal Z", Material.ARROW, builder);
        addDirectionButton(inv, 22, GradientDirection.RADIAL, "Radial", Material.TARGET, builder);

        addModeButton(inv, 27, InterpolationMode.LINEAR, "Linear", Material.IRON_INGOT, builder);
        addModeButton(inv, 28, InterpolationMode.SMOOTH, "Smooth", Material.GOLD_INGOT, builder);
        addModeButton(inv, 29, InterpolationMode.DISCRETE, "Discrete", Material.DIAMOND, builder);

        renderLegacyPreview(inv, builder);

        ItemStack apply = new ItemStack(Material.EMERALD);
        ItemMeta applyMeta = apply.getItemMeta();
        applyMeta.displayName(
                Component.text("Apply Gradient").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        applyMeta.lore(Arrays.asList(
                Component.text("Apply this gradient to your").color(NamedTextColor.GRAY),
                Component.text("current WorldEdit selection").color(NamedTextColor.GRAY)));
        apply.setItemMeta(applyMeta);
        inv.setItem(49, apply);

        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(53, cancel);

        return inv;
    }

    private void addDirectionButton(Inventory inv, int slot, GradientDirection direction,
            String name, Material material, GradientBuilder builder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean isSelected = builder.direction == direction;
        NamedTextColor color = isSelected ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        Component nameComp = Component.text(name).color(color);
        if (isSelected)
            nameComp = nameComp.decorate(TextDecoration.BOLD);
        meta.displayName(nameComp);

        if (isSelected) {
            meta.lore(Arrays.asList(Component.text("Selected").color(NamedTextColor.GREEN)));
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addModeButton(Inventory inv, int slot, InterpolationMode mode,
            String name, Material material, GradientBuilder builder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean isSelected = builder.interpolationMode == mode;
        NamedTextColor color = isSelected ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        Component nameComp = Component.text(name).color(color);
        if (isSelected)
            nameComp = nameComp.decorate(TextDecoration.BOLD);
        meta.displayName(nameComp);

        if (isSelected) {
            meta.lore(Arrays.asList(Component.text("Selected").color(NamedTextColor.GREEN)));
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void renderLegacyPreview(Inventory inv, GradientBuilder builder) {
        if (builder.stops.size() < 2) {
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = placeholder.getItemMeta();
            meta.displayName(Component.text("Add at least 2 blocks").color(NamedTextColor.GRAY));
            placeholder.setItemMeta(meta);
            for (int i = 36; i <= 44; i++) {
                inv.setItem(i, placeholder);
            }
            return;
        }

        try {
            GradientDefinition gradient = builder.build();

            for (int i = 0; i < 9; i++) {
                double position = i / 8.0;
                BlockType blockType = gradient.getBlockAt(position);

                Material material = Material.matchMaterial(blockType.id().replace("minecraft:", "").toUpperCase());
                if (material != null) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text((int) (position * 100) + "%").color(NamedTextColor.AQUA));
                    item.setItemMeta(meta);
                    inv.setItem(36 + i, item);
                }
            }
        } catch (Exception e) {
        }
    }

    private void openLegacyBlockSelector(Player player, int stopIndex) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(LEGACY_BLOCK_SELECTOR_TITLE).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));

        List<Material> commonBlocks = Arrays.asList(
                Material.STONE, Material.COBBLESTONE, Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.SANDSTONE, Material.RED_SANDSTONE, Material.SMOOTH_STONE,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
                Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL,
                Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
                Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL,
                Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL, Material.BROWN_WOOL,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE,
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE,
                Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE, Material.BROWN_CONCRETE,
                Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.CLAY);

        for (int i = 0; i < Math.min(commonBlocks.size(), 45); i++) {
            ItemStack item = new ItemStack(commonBlocks.get(i));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(commonBlocks.get(i).name().toLowerCase().replace("_", " "))
                    .color(NamedTextColor.YELLOW));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(53, back);

        player.openInventory(inv);

        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "editing_stop"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                stopIndex);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains(LEGACY_BUILDER_TITLE) || title.contains(MODE_SELECTION_TITLE)) {
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    activeBuilders.remove(player.getUniqueId());
                }
            }, 6000L);
        }
    }

    private void applyLegacyGradient(Player player, GradientBuilder builder) {
        try {
            if (builder.stops.size() < 2) {
                MessageManager.error(player, "Please add at least 2 gradient stops.");
                return;
            }

            GradientDefinition gradient = builder.build();
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            player.closeInventory();
            MessageManager.info(player, "Applying gradient...");

            int affected = gradientCommand.applyGradient(actor, region, gradient);

            MessageManager.success(player, "Gradient applied to %d blocks.", affected);
            activeBuilders.remove(player.getUniqueId());

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "Error applying gradient: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    private static class GradientBuilder {
        List<BlockType> stops = new ArrayList<>();
        GradientDirection direction = GradientDirection.VERTICAL_UP;
        InterpolationMode interpolationMode = InterpolationMode.LINEAR;

        GradientDefinition build() {
            List<GradientStop> gradientStops = new ArrayList<>();
            for (int i = 0; i < stops.size(); i++) {
                double position = stops.size() > 1 ? (double) i / (stops.size() - 1) : 0.5;
                gradientStops.add(new GradientStop(stops.get(i), position));
            }
            return new GradientDefinition(gradientStops, direction, interpolationMode);
        }
    }
}
