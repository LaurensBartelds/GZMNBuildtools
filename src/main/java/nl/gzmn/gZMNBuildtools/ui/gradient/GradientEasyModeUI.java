package nl.gzmn.gZMNBuildtools.ui.gradient;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.config.GradientPresets;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.gradient.service.GradientExecutor;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
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
import java.util.stream.Collectors;


public class GradientEasyModeUI implements Listener {

    private static final String PRESET_GALLERY_TITLE = "Gradient Presets";
    private static final String DIRECTION_TITLE = "Select Direction";
    private static final String NOISE_SETTINGS_TITLE = "Basic Noise Settings";

    private final Plugin plugin;
    private final GradientPreviewRenderer previewRenderer;
    private final Map<UUID, EasyModeBuilder> activeBuilders = new HashMap<>();
    private GradientStorageManager storageManager;

    public GradientEasyModeUI(Plugin plugin, GradientPreviewRenderer previewRenderer) {
        this.plugin = plugin;
        this.previewRenderer = previewRenderer;
    }

    public void setStorageManager(GradientStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openPresetGallery(Player player) {
        openPresetGallery(player, GalleryTab.SYSTEM, 0);
    }

    public void openPresetGallery(Player player, GalleryTab tab, int page) {
        EasyModeBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), k -> new EasyModeBuilder());
        builder.currentTab = tab;
        builder.page = page;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(PRESET_GALLERY_TITLE).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        
        addTabButton(inv, 0, GalleryTab.SYSTEM, "System Presets", Material.BOOK, builder.currentTab);
        addTabButton(inv, 1, GalleryTab.SAVED, "My Saved", Material.WRITABLE_BOOK, builder.currentTab);
        addTabButton(inv, 2, GalleryTab.SHARED, "Shared", Material.ENDER_CHEST, builder.currentTab);

        
        ItemStack advanced = new ItemStack(Material.DIAMOND);
        ItemMeta advMeta = advanced.getItemMeta();
        advMeta.displayName(
                Component.text("Advanced Mode").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        advMeta.lore(Arrays.asList(Component.text("Switch to full control").color(NamedTextColor.GRAY)));
        advanced.setItemMeta(advMeta);
        inv.setItem(8, advanced);

        
        List<GradientPreset> presets = getPresetsForTab(player, tab);
        int startIndex = page * 36;
        int endIndex = Math.min(startIndex + 36, presets.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            GradientPreset preset = presets.get(i);
            ItemStack item = new ItemStack(preset.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(preset.getDisplayName()).color(NamedTextColor.YELLOW));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(preset.getDescription()).color(NamedTextColor.GRAY));
            lore.add(Component.text("Category: " + preset.getCategory().getDisplayName())
                    .color(NamedTextColor.DARK_GRAY));

            if (tab == GalleryTab.SAVED) {
                lore.add(Component.empty());
                lore.add(Component.text("Right-click to share/delete").color(NamedTextColor.RED));
            }

            lore.add(Component.empty());
            lore.add(Component.text("Click to select").color(NamedTextColor.GREEN));

            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        
        int totalPages = (int) Math.ceil(presets.size() / 36.0);
        if (totalPages == 0)
            totalPages = 1;

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("← Previous Page").color(NamedTextColor.GOLD));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.displayName(Component.text("Page " + (page + 1) + "/" + totalPages).color(NamedTextColor.YELLOW));
        pageMeta.lore(Arrays.asList(
                Component.text(presets.size() + " presets total").color(NamedTextColor.GRAY)));
        pageIndicator.setItemMeta(pageMeta);
        inv.setItem(49, pageIndicator);

        if (endIndex < presets.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next Page →").color(NamedTextColor.GOLD));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    private void addTabButton(Inventory inv, int slot, GalleryTab tab, String name, Material mat, GalleryTab current) {
        boolean selected = tab == current;
        ItemStack item = new ItemStack(selected ? Material.ENCHANTED_BOOK : mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name)
                .color(selected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decorate(selected ? TextDecoration.BOLD : TextDecoration.OBFUSCATED)
                .decoration(TextDecoration.OBFUSCATED, false));
        if (selected) {
            meta.lore(Arrays.asList(Component.text("Currently viewing").color(NamedTextColor.GRAY)));
        }
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private List<GradientPreset> getPresetsForTab(Player player, GalleryTab tab) {
        switch (tab) {
            case SYSTEM:
                return GradientPresets.getAll();
            case SAVED:
                if (storageManager == null)
                    return Collections.emptyList();
                return storageManager.getPlayerGradients(player.getUniqueId()).stream()
                        .map(SavedGradient::getPreset)
                        .collect(Collectors.toList());
            case SHARED:
                if (storageManager == null)
                    return Collections.emptyList();
                
                
                return storageManager.getGlobalGradients().stream()
                        .map(SavedGradient::getPreset)
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    public void openDirectionSelection(Player player) {
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || builder.selectedPreset == null) {
            openPresetGallery(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(DIRECTION_TITLE).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back to Presets").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        ItemStack presetInfo = new ItemStack(builder.selectedPreset.getIcon());
        ItemMeta presetMeta = presetInfo.getItemMeta();
        presetMeta.displayName(Component.text("Selected: " + builder.selectedPreset.getDisplayName())
                .color(NamedTextColor.GOLD));
        presetInfo.setItemMeta(presetMeta);
        inv.setItem(4, presetInfo);

        addSelectionStatus(inv, 8, player);

        
        addDirectionButton(inv, 20, GradientDefinition.GradientDirection.VERTICAL_UP, "Up", Material.ARROW, builder);
        addDirectionButton(inv, 21, GradientDefinition.GradientDirection.VERTICAL_DOWN, "Down", Material.ARROW,
                builder);
        addDirectionButton(inv, 22, GradientDefinition.GradientDirection.HORIZONTAL_X, "X", Material.ARROW, builder);
        addDirectionButton(inv, 23, GradientDefinition.GradientDirection.HORIZONTAL_Z, "Z", Material.ARROW, builder);
        addDirectionButton(inv, 24, GradientDefinition.GradientDirection.RADIAL, "Radial", Material.TARGET, builder);

        
        ItemStack noiseToggle = new ItemStack(builder.useNoise ? Material.CHORUS_FRUIT : Material.PAPER);
        ItemMeta noiseMeta = noiseToggle.getItemMeta();
        noiseMeta.displayName(Component.text("Noise: " + (builder.useNoise ? "ON" : "OFF"))
                .color(builder.useNoise ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.WHITE));
        noiseMeta.lore(Arrays.asList(
                Component.text("Click to toggle noise").color(NamedTextColor.GRAY),
                Component.text("Right-click for settings").color(NamedTextColor.DARK_GRAY)));
        noiseToggle.setItemMeta(noiseMeta);
        inv.setItem(31, noiseToggle);

        
        GradientDefinition previewDef = GradientDefinition.fromPreset(builder.selectedPreset, builder.direction);
        if (builder.useNoise) {
            previewRenderer.renderPreviewBar(inv, 36, previewDef);
            
        } else {
            previewRenderer.renderPreviewBar(inv, 36, builder.selectedPreset, builder.direction);
        }

        ItemStack apply = new ItemStack(Material.EMERALD);
        ItemMeta applyMeta = apply.getItemMeta();
        applyMeta.displayName(
                Component.text("Apply Gradient").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        apply.setItemMeta(applyMeta);
        inv.setItem(49, apply);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

        player.openInventory(inv);
    }

    private void addDirectionButton(Inventory inv, int slot, GradientDefinition.GradientDirection direction,
            String name, Material material, EasyModeBuilder builder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        boolean isSelected = builder.direction == direction;
        NamedTextColor color = isSelected ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        Component nameComp = Component.text(name).color(color);
        if (isSelected)
            nameComp = nameComp.decorate(TextDecoration.BOLD);
        meta.displayName(nameComp);
        if (isSelected)
            meta.lore(Arrays.asList(Component.text("Selected").color(NamedTextColor.GREEN)));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public void openNoiseSettings(Player player) {
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(NOISE_SETTINGS_TITLE).color(NamedTextColor.LIGHT_PURPLE));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        
        ItemStack scale = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta scaleMeta = scale.getItemMeta();
        scaleMeta.displayName(Component.text("Scale: " + String.format("%.2f", builder.noiseSettings.getScale()))
                .color(NamedTextColor.AQUA));
        scaleMeta.lore(Arrays.asList(
                Component.text("Left-click: Decrease").color(NamedTextColor.GRAY),
                Component.text("Right-click: Increase").color(NamedTextColor.GRAY)));
        scale.setItemMeta(scaleMeta);
        inv.setItem(11, scale);

        ItemStack strength = new ItemStack(Material.REDSTONE);
        ItemMeta strMeta = strength.getItemMeta();
        strMeta.displayName(Component.text("Strength: " + String.format("%.2f", builder.noiseSettings.getStrength()))
                .color(NamedTextColor.RED));
        strMeta.lore(Arrays.asList(
                Component.text("Left-click: Decrease").color(NamedTextColor.GRAY),
                Component.text("Right-click: Increase").color(NamedTextColor.GRAY)));
        strength.setItemMeta(strMeta);
        inv.setItem(15, strength);

        player.openInventory(inv);
    }

    private void addSelectionStatus(Inventory inv, int slot, Player player) {
        boolean hasSelection = hasWorldEditSelection(player);
        ItemStack status = new ItemStack(hasSelection ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = status.getItemMeta();
        if (hasSelection) {
            meta.displayName(Component.text("Selection Ready").color(NamedTextColor.GREEN));
        } else {
            meta.displayName(Component.text("No Selection").color(NamedTextColor.RED));
        }
        status.setItemMeta(meta);
        inv.setItem(slot, status);
    }

    private boolean hasWorldEditSelection(Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());
            return region != null;
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains(PRESET_GALLERY_TITLE)) {
            handlePresetGalleryClick(event, player);
        } else if (title.contains(DIRECTION_TITLE)) {
            handleDirectionClick(event, player);
        } else if (title.contains(NOISE_SETTINGS_TITLE)) {
            handleNoiseSettingsClick(event, player);
        }
    }

    private void handlePresetGalleryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;
        int slot = event.getSlot();
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        
        if (slot == 0) {
            openPresetGallery(player, GalleryTab.SYSTEM, 0);
            return;
        }
        if (slot == 1) {
            openPresetGallery(player, GalleryTab.SAVED, 0);
            return;
        }
        if (slot == 2) {
            openPresetGallery(player, GalleryTab.SHARED, 0);
            return;
        }

        if (slot == 8) {
            
            player.closeInventory();
            
            
            
            
            
            MessageManager.info(player, "Use /gradient advanced to open Advanced Mode.");
            return;
        }

        if (slot == 45 && builder.page > 0) {
            openPresetGallery(player, builder.currentTab, builder.page - 1);
            return;
        }
        if (slot == 53) { 
            openPresetGallery(player, builder.currentTab, builder.page + 1);
            return;
        }

        if (slot >= 9 && slot < 45) {
            List<GradientPreset> presets = getPresetsForTab(player, builder.currentTab);
            int idx = (builder.page * 36) + (slot - 9);
            if (idx < presets.size()) {
                GradientPreset preset = presets.get(idx);

                if (builder.currentTab == GalleryTab.SAVED && event.isRightClick()) {
                    List<SavedGradient> savedList = storageManager.getPlayerGradients(player.getUniqueId());
                    
                    
                    
                    
                    
                    
                    Optional<SavedGradient> savedOpt = savedList.stream()
                            .filter(g -> g.getPreset().getId().equals(preset.getId()))
                            .findFirst();

                    if (savedOpt.isPresent()) {
                        SavedGradient saved = savedOpt.get();
                        if (event.isShiftClick()) {
                            
                            storageManager.deleteGradient(player.getUniqueId(), saved.getId());
                            MessageManager.success(player, "Deleted gradient '%s'", preset.getDisplayName());
                            openPresetGallery(player, builder.currentTab, builder.page);
                        } else {
                            
                            
                            
                            
                            
                            
                            storageManager.deleteGradient(player.getUniqueId(), saved.getId());
                            MessageManager.success(player, "Deleted gradient '%s'", preset.getDisplayName());
                            openPresetGallery(player, builder.currentTab, builder.page);
                        }
                    }
                    return;
                }

                builder.selectedPreset = preset;
                openDirectionSelection(player);
            }
        }
    }

    private void handleDirectionClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;
        int slot = event.getSlot();
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        if (slot == 0) {
            openPresetGallery(player, builder.currentTab, builder.page);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            activeBuilders.remove(player.getUniqueId());
            return;
        }

        if (slot >= 20 && slot <= 24) {
            GradientDefinition.GradientDirection[] directions = GradientDefinition.GradientDirection.values();
            int dirIdx = slot - 20;
            if (dirIdx < directions.length) {
                builder.direction = directions[dirIdx];
                openDirectionSelection(player);
            }
            return;
        }

        if (slot == 31) {
            if (event.isRightClick()) {
                openNoiseSettings(player);
            } else {
                builder.useNoise = !builder.useNoise;
                openDirectionSelection(player);
            }
            return;
        }

        if (slot == 49) {
            applyGradient(player, builder);
        }
    }

    private void handleNoiseSettingsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;
        int slot = event.getSlot();
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());

        if (slot == 0) {
            openDirectionSelection(player);
            return;
        }

        if (slot == 11) {
            double val = builder.noiseSettings.getScale();
            val += event.isRightClick() ? 0.05 : -0.05;
            val = Math.max(0.01, Math.min(0.5, val));
            builder.noiseSettings = builder.noiseSettings.withScale(val);
            openNoiseSettings(player);
        } else if (slot == 15) {
            double val = builder.noiseSettings.getStrength();
            val += event.isRightClick() ? 0.1 : -0.1;
            val = Math.max(0.0, Math.min(1.0, val));
            builder.noiseSettings = builder.noiseSettings.withStrength(val);
            openNoiseSettings(player);
        }
    }

    private void applyGradient(Player player, EasyModeBuilder builder) {
        if (builder.selectedPreset == null)
            return;

        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            player.closeInventory();
            MessageManager.info(player, "Applying gradient...");

            GradientExecutor.GradientResult result;
            if (builder.useNoise) {
                
                
                GradientDefinition def = GradientDefinition.fromPreset(builder.selectedPreset, builder.direction);
                result = GradientExecutor.getInstance().applyNoise(actor, region, def, builder.noiseSettings);
            } else {
                result = GradientExecutor.getInstance().applyPreset(
                        actor, region, builder.selectedPreset, builder.direction, false, null);
            }

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

            activeBuilders.remove(player.getUniqueId());

        } catch (Exception e) {
            MessageManager.error(player, "Error: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains(PRESET_GALLERY_TITLE) || title.contains(DIRECTION_TITLE)) {
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    activeBuilders.remove(player.getUniqueId());
                }
            }, 6000L);
        }
    }

    private static class EasyModeBuilder {
        GalleryTab currentTab = GalleryTab.SYSTEM;
        int page = 0;
        GradientPreset selectedPreset = null;
        GradientDefinition.GradientDirection direction = GradientDefinition.GradientDirection.VERTICAL_UP;
        boolean useNoise = false;
        NoiseSettings noiseSettings = NoiseSettings.defaults();
    }

    public enum GalleryTab {
        SYSTEM, SAVED, SHARED
    }
}
