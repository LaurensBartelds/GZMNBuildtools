package nl.gzmn.gZMNBuildtools.ui;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.gzmn.gZMNBuildtools.commands.GradientCommand;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition;
import nl.gzmn.gZMNBuildtools.util.MessageManager;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientDirection;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.GradientStop;
import nl.gzmn.gZMNBuildtools.gradient.GradientDefinition.InterpolationMode;
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
 * Manages the gradient UI for visual gradient creation
 */
public class GradientUIManager implements Listener {

    private final Plugin plugin;
    private final GradientCommand gradientCommand;
    private final Map<UUID, GradientBuilder> activeBuilders;

    public GradientUIManager(Plugin plugin, GradientCommand gradientCommand) {
        this.plugin = plugin;
        this.gradientCommand = gradientCommand;
        this.activeBuilders = new HashMap<>();
    }

    /**
     * Register event listeners. Must be called after plugin is enabled.
     */
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the gradient UI for a player
     */
    public void openGradientUI(Player player) {
        GradientBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(),
                k -> new GradientBuilder());

        Inventory inventory = createGradientInventory(builder);
        player.openInventory(inventory);
    }

    /**
     * Create the gradient configuration inventory
     */
    private Inventory createGradientInventory(GradientBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 54,
                MessageManager.asLegacyString(Component.text("Gradient Builder").color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD)));

        // Gradient stops (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (i < builder.stops.size()) {
                BlockType blockType = builder.stops.get(i);
                Material material = Material.matchMaterial(blockType.id().replace("minecraft:", "").toUpperCase());
                if (material != null) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(MessageManager.asLegacyString(
                            Component.text("Stop " + (i + 1) + ": " + blockType.id()).color(NamedTextColor.YELLOW)));
                    meta.setLore(Arrays.asList(
                            MessageManager
                                    .asLegacyString(Component.text("Left-click to change").color(NamedTextColor.GRAY)),
                            MessageManager.asLegacyString(
                                    Component.text("Right-click to remove").color(NamedTextColor.GRAY))));
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }
            } else {
                ItemStack addStop = new ItemStack(Material.LIME_DYE);
                ItemMeta meta = addStop.getItemMeta();
                meta.setDisplayName(
                        MessageManager.asLegacyString(Component.text("Add Gradient Stop").color(NamedTextColor.GREEN)));
                meta.setLore(Arrays.asList(MessageManager
                        .asLegacyString(Component.text("Click to add a block").color(NamedTextColor.GRAY))));
                addStop.setItemMeta(meta);
                inv.setItem(i, addStop);
                break;
            }
        }

        // Direction selector (slots 18-22)
        addDirectionButton(inv, 18, GradientDirection.VERTICAL_UP, "↑ Vertical Up", Material.ARROW, builder);
        addDirectionButton(inv, 19, GradientDirection.VERTICAL_DOWN, "↓ Vertical Down", Material.ARROW, builder);
        addDirectionButton(inv, 20, GradientDirection.HORIZONTAL_X, "→ Horizontal X", Material.ARROW, builder);
        addDirectionButton(inv, 21, GradientDirection.HORIZONTAL_Z, "→ Horizontal Z", Material.ARROW, builder);
        addDirectionButton(inv, 22, GradientDirection.RADIAL, "◉ Radial", Material.TARGET, builder);

        // Interpolation mode (slots 27-29)
        addModeButton(inv, 27, InterpolationMode.LINEAR, "Linear", Material.IRON_INGOT, builder);
        addModeButton(inv, 28, InterpolationMode.SMOOTH, "Smooth", Material.GOLD_INGOT, builder);
        addModeButton(inv, 29, InterpolationMode.DISCRETE, "Discrete", Material.DIAMOND, builder);

        // Preview area (slots 36-44)
        renderPreview(inv, builder);

        // Apply button (slot 49)
        ItemStack apply = new ItemStack(Material.EMERALD);
        ItemMeta applyMeta = apply.getItemMeta();
        applyMeta.setDisplayName(MessageManager.asLegacyString(
                Component.text("Apply Gradient").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)));
        applyMeta.setLore(Arrays.asList(
                MessageManager.asLegacyString(Component.text("Apply this gradient to your").color(NamedTextColor.GRAY)),
                MessageManager
                        .asLegacyString(Component.text("current WorldEdit selection").color(NamedTextColor.GRAY))));
        apply.setItemMeta(applyMeta);
        inv.setItem(49, apply);

        // Cancel button (slot 53)
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(MessageManager.asLegacyString(Component.text("Close").color(NamedTextColor.RED)));
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
        meta.setDisplayName(MessageManager.asLegacyString(nameComp));

        if (isSelected) {
            meta.setLore(Arrays
                    .asList(MessageManager.asLegacyString(Component.text("✓ Selected").color(NamedTextColor.GREEN))));
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
        meta.setDisplayName(MessageManager.asLegacyString(nameComp));

        if (isSelected) {
            meta.setLore(Arrays
                    .asList(MessageManager.asLegacyString(Component.text("✓ Selected").color(NamedTextColor.GREEN))));
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void renderPreview(Inventory inv, GradientBuilder builder) {
        if (builder.stops.size() < 2) {
            // Show placeholder
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = placeholder.getItemMeta();
            meta.setDisplayName(
                    MessageManager.asLegacyString(Component.text("Add at least 2 blocks").color(NamedTextColor.GRAY)));
            placeholder.setItemMeta(meta);
            for (int i = 36; i <= 44; i++) {
                inv.setItem(i, placeholder);
            }
            return;
        }

        try {
            GradientDefinition gradient = builder.build();

            // Preview 9 steps of the gradient
            for (int i = 0; i < 9; i++) {
                double position = i / 8.0;
                BlockType blockType = gradient.getBlockAt(position);

                Material material = Material.matchMaterial(blockType.id().replace("minecraft:", "").toUpperCase());
                if (material != null) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(MessageManager
                            .asLegacyString(Component.text((int) (position * 100) + "%").color(NamedTextColor.AQUA)));
                    item.setItemMeta(meta);
                    inv.setItem(36 + i, item);
                }
            }
        } catch (Exception e) {
            // Error in gradient, show error indicator
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = event.getView().getTitle();
        if (!title.contains("Gradient Builder")) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        GradientBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        int slot = event.getSlot();

        // Handle gradient stop clicks (0-8)
        if (slot >= 0 && slot < 9) {
            if (event.getCurrentItem().getType() == Material.LIME_DYE) {
                // Add new stop - open block selector
                player.closeInventory();
                openBlockSelector(player, builder.stops.size());
            } else if (event.isRightClick() && slot < builder.stops.size()) {
                // Remove stop
                builder.stops.remove(slot);
                player.openInventory(createGradientInventory(builder));
            }
        }
        // Handle direction clicks (18-22)
        else if (slot >= 18 && slot <= 22) {
            GradientDirection[] directions = GradientDirection.values();
            int dirIndex = slot - 18;
            if (dirIndex < directions.length) {
                builder.direction = directions[dirIndex];
                player.openInventory(createGradientInventory(builder));
            }
        }
        // Handle mode clicks (27-29)
        else if (slot >= 27 && slot <= 29) {
            InterpolationMode[] modes = InterpolationMode.values();
            int modeIndex = slot - 27;
            if (modeIndex < modes.length) {
                builder.interpolationMode = modes[modeIndex];
                player.openInventory(createGradientInventory(builder));
            }
        }
        // Handle apply button (49)
        else if (slot == 49) {
            applyGradient(player, builder);
        }
        // Handle cancel button (53)
        else if (slot == 53) {
            player.closeInventory();
        }
    }

    private void openBlockSelector(Player player, int stopIndex) {
        Inventory inv = Bukkit.createInventory(null, 54,
                MessageManager.asLegacyString(
                        Component.text("Select Block").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD)));

        // Common building blocks
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
            meta.setDisplayName(MessageManager.asLegacyString(Component
                    .text(commonBlocks.get(i).name().toLowerCase().replace("_", " ")).color(NamedTextColor.YELLOW)));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(MessageManager.asLegacyString(Component.text("← Back").color(NamedTextColor.GRAY)));
        back.setItemMeta(backMeta);
        inv.setItem(53, back);

        player.openInventory(inv);

        // Store the stop index we're editing
        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "editing_stop"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                stopIndex);
    }

    @EventHandler
    public void onBlockSelectorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = event.getView().getTitle();
        if (!title.contains("Select Block")) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        GradientBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        int slot = event.getSlot();

        // Back button
        if (slot == 53) {
            player.openInventory(createGradientInventory(builder));
            return;
        }

        // Block selection
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

            player.openInventory(createGradientInventory(builder));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Gradient Builder")) {
            // Keep the builder for 5 minutes in case they reopen
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only remove if they haven't reopened
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    activeBuilders.remove(player.getUniqueId());
                }
            }, 6000L); // 5 minutes
        }
    }

    private void applyGradient(Player player, GradientBuilder builder) {
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
            MessageManager.info(player, "Applying %s", "gradient");

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

    /**
     * Internal class to build a gradient configuration
     */
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
