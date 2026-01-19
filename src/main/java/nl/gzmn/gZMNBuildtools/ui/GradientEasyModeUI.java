package nl.gzmn.gZMNBuildtools.ui;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.gradient.*;
import nl.gzmn.gZMNBuildtools.util.MessageManager;
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

/**
 * Easy Mode UI for gradient creation.
 * Provides a simple preset-based workflow: Select preset -> Select direction -> Apply
 */
public class GradientEasyModeUI implements Listener {

    private static final String PRESET_GALLERY_TITLE = "Gradient Presets";
    private static final String DIRECTION_TITLE = "Select Direction";

    private final Plugin plugin;
    private final GradientPreviewRenderer previewRenderer;
    private final Map<UUID, EasyModeBuilder> activeBuilders = new HashMap<>();

    public GradientEasyModeUI(Plugin plugin, GradientPreviewRenderer previewRenderer) {
        this.plugin = plugin;
        this.previewRenderer = previewRenderer;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the preset gallery (first page of easy mode).
     */
    public void openPresetGallery(Player player) {
        activeBuilders.computeIfAbsent(player.getUniqueId(), k -> new EasyModeBuilder());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(PRESET_GALLERY_TITLE).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back to Mode Selection").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Easy Mode").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        infoMeta.lore(Arrays.asList(
                Component.text("Select a preset to quickly").color(NamedTextColor.GRAY),
                Component.text("apply a gradient.").color(NamedTextColor.GRAY)
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        addSelectionStatus(inv, 8, player);

        List<GradientPreset> presets = GradientPresets.getAll();
        int slot = 9;
        for (GradientPreset preset : presets) {
            if (slot >= 45) break;

            ItemStack item = new ItemStack(preset.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(preset.getDisplayName()).color(NamedTextColor.YELLOW));
            meta.lore(Arrays.asList(
                    Component.text(preset.getDescription()).color(NamedTextColor.GRAY),
                    Component.text("Category: " + preset.getCategory().getDisplayName()).color(NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("Click to select").color(NamedTextColor.GREEN)
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder != null && builder.selectedPreset != null) {
            previewRenderer.renderPreviewBar(inv, 45, builder.selectedPreset,
                    GradientDefinition.GradientDirection.VERTICAL_UP);
        } else {
            previewRenderer.renderEmptyPreview(inv, 45);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

        player.openInventory(inv);
    }

    /**
     * Open the direction selection page.
     */
    public void openDirectionSelection(Player player) {
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || builder.selectedPreset == null) {
            MessageManager.error(player, "Please select a preset first.");
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

        addDirectionButton(inv, 20, GradientDefinition.GradientDirection.VERTICAL_UP,
                "Vertical Up", Material.ARROW, builder);
        addDirectionButton(inv, 21, GradientDefinition.GradientDirection.VERTICAL_DOWN,
                "Vertical Down", Material.ARROW, builder);
        addDirectionButton(inv, 22, GradientDefinition.GradientDirection.HORIZONTAL_X,
                "Horizontal X", Material.ARROW, builder);
        addDirectionButton(inv, 23, GradientDefinition.GradientDirection.HORIZONTAL_Z,
                "Horizontal Z", Material.ARROW, builder);
        addDirectionButton(inv, 24, GradientDefinition.GradientDirection.RADIAL,
                "Radial", Material.TARGET, builder);

        previewRenderer.renderPreviewBar(inv, 36, builder.selectedPreset, builder.direction);

        ItemStack apply = new ItemStack(Material.EMERALD);
        ItemMeta applyMeta = apply.getItemMeta();
        applyMeta.displayName(Component.text("Apply Gradient").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        applyMeta.lore(Arrays.asList(
                Component.text("Apply to your WorldEdit selection").color(NamedTextColor.GRAY)
        ));
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
        if (isSelected) nameComp = nameComp.decorate(TextDecoration.BOLD);
        meta.displayName(nameComp);

        if (isSelected) {
            meta.lore(Arrays.asList(Component.text("Selected").color(NamedTextColor.GREEN)));
        } else {
            meta.lore(Arrays.asList(Component.text("Click to select").color(NamedTextColor.GRAY)));
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addSelectionStatus(Inventory inv, int slot, Player player) {
        boolean hasSelection = hasWorldEditSelection(player);

        ItemStack status = new ItemStack(hasSelection ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = status.getItemMeta();
        if (hasSelection) {
            meta.displayName(Component.text("Selection Ready").color(NamedTextColor.GREEN));
            meta.lore(Arrays.asList(Component.text("WorldEdit selection detected").color(NamedTextColor.GRAY)));
        } else {
            meta.displayName(Component.text("No Selection").color(NamedTextColor.RED));
            meta.lore(Arrays.asList(Component.text("Use WorldEdit to select an area").color(NamedTextColor.GRAY)));
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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains(PRESET_GALLERY_TITLE)) {
            handlePresetGalleryClick(event, player);
        } else if (title.contains(DIRECTION_TITLE)) {
            handleDirectionClick(event, player);
        }
    }

    private void handlePresetGalleryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return;

        if (slot == 0) {
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            activeBuilders.remove(player.getUniqueId());
            return;
        }

        if (slot >= 9 && slot < 45) {
            List<GradientPreset> presets = GradientPresets.getAll();
            int presetIndex = slot - 9;
            if (presetIndex < presets.size()) {
                builder.selectedPreset = presets.get(presetIndex);
                openDirectionSelection(player);
            }
        }
    }

    private void handleDirectionClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        EasyModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return;

        if (slot == 0) {
            openPresetGallery(player);
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            activeBuilders.remove(player.getUniqueId());
            return;
        }

        GradientDefinition.GradientDirection[] directions = {
                GradientDefinition.GradientDirection.VERTICAL_UP,
                GradientDefinition.GradientDirection.VERTICAL_DOWN,
                GradientDefinition.GradientDirection.HORIZONTAL_X,
                GradientDefinition.GradientDirection.HORIZONTAL_Z,
                GradientDefinition.GradientDirection.RADIAL
        };

        if (slot >= 20 && slot <= 24) {
            int dirIndex = slot - 20;
            builder.direction = directions[dirIndex];
            openDirectionSelection(player);
            return;
        }

        if (slot == 49) {
            applyGradient(player, builder);
        }
    }

    private void applyGradient(Player player, EasyModeBuilder builder) {
        if (builder.selectedPreset == null) {
            MessageManager.error(player, "Please select a preset first.");
            return;
        }

        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());

            if (region == null) {
                MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
                return;
            }

            player.closeInventory();
            MessageManager.info(player, "Applying gradient...");

            GradientExecutor.GradientResult result = GradientExecutor.getInstance().applyPreset(
                    actor, region, builder.selectedPreset, builder.direction, false, null);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

            activeBuilders.remove(player.getUniqueId());

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "Error applying gradient: %s", e.getMessage());
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

    /**
     * Get the back action to return to mode selection.
     */
    public Runnable getBackToModeSelection(Player player) {
        return () -> {
            activeBuilders.remove(player.getUniqueId());
        };
    }

    private static class EasyModeBuilder {
        GradientPreset selectedPreset = null;
        GradientDefinition.GradientDirection direction = GradientDefinition.GradientDirection.VERTICAL_UP;
    }
}
