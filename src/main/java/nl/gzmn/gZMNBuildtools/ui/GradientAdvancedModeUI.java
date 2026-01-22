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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.gradient.*;
import nl.gzmn.gZMNBuildtools.noise.NoiseSettings;
import nl.gzmn.gZMNBuildtools.util.MessageManager;
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

/**
 * Advanced Mode UI for gradient creation.
 * Provides full control over gradient stops, direction, interpolation, and
 * noise settings.
 */
public class GradientAdvancedModeUI implements Listener {

    private static final String MAIN_TITLE = "Advanced Gradient";
    private static final String BLOCK_SELECTOR_TITLE = "Select Block";
    private static final String NOISE_SETTINGS_TITLE = "Noise Settings";

    private final Plugin plugin;
    private final GradientPreviewRenderer previewRenderer;
    private final Map<UUID, AdvancedModeBuilder> activeBuilders = new HashMap<>();

    private final NamespacedKey editingStopKey;
    private nl.gzmn.gZMNBuildtools.gradient.GradientStorageManager storageManager;
    private GradientBlockBrowser blockBrowser;

    public GradientAdvancedModeUI(Plugin plugin, GradientPreviewRenderer previewRenderer) {
        this.plugin = plugin;
        this.previewRenderer = previewRenderer;
        this.editingStopKey = new NamespacedKey(plugin, "advanced_editing_stop");
        this.blockBrowser = new GradientBlockBrowser(plugin);
    }

    public void setStorageManager(nl.gzmn.gZMNBuildtools.gradient.GradientStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        blockBrowser.registerEvents();
    }

    /**
     * Open the main advanced mode UI.
     */
    public void openMainUI(Player player) {
        AdvancedModeBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(),
                k -> new AdvancedModeBuilder());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(MAIN_TITLE).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back to Basic Mode").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        ItemStack info = new ItemStack(Material.DIAMOND);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(
                Component.text("Advanced Mode").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        infoMeta.lore(Arrays.asList(
                Component.text("Full control over your gradient").color(NamedTextColor.GRAY)));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        addSelectionStatus(inv, 8, player);

        for (int i = 0; i < 9; i++) {
            if (i < builder.stops.size()) {
                List<BlockType> blockTypes = builder.stops.get(i);
                BlockType primaryBlock = blockTypes.get(0);
                Material material = getMaterialFromBlockType(primaryBlock);
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                String displayName = builder.getStopDisplayString(i);
                meta.displayName(Component.text("Stop " + (i + 1) + ": " + displayName)
                        .color(NamedTextColor.YELLOW));

                List<Component> lore = new ArrayList<>();
                if (blockTypes.size() > 1) {
                    lore.add(Component.text(blockTypes.size() + " blocks (random)").color(NamedTextColor.AQUA));
                }
                lore.add(Component.text("Click to change").color(NamedTextColor.GRAY));
                lore.add(Component.text("Shift+click for multi-block").color(NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Right-click to remove").color(NamedTextColor.GRAY));
                meta.lore(lore);

                item.setItemMeta(meta);
                inv.setItem(9 + i, item);
            } else if (i == builder.stops.size() && builder.stops.size() < 9) {
                ItemStack addStop = new ItemStack(Material.LIME_DYE);
                ItemMeta meta = addStop.getItemMeta();
                meta.displayName(Component.text("Add Gradient Stop").color(NamedTextColor.GREEN));
                meta.lore(Arrays.asList(
                        Component.text("Click to add a block").color(NamedTextColor.GRAY),
                        Component.text("Shift+click for multi-block").color(NamedTextColor.DARK_GRAY),
                        Component.text("(random selection per position)").color(NamedTextColor.DARK_GRAY)));
                addStop.setItemMeta(meta);
                inv.setItem(9 + i, addStop);
                break;
            }
        }

        addDirectionButton(inv, 18, GradientDefinition.GradientDirection.VERTICAL_UP, "Up", Material.ARROW, builder);
        addDirectionButton(inv, 19, GradientDefinition.GradientDirection.VERTICAL_DOWN, "Down", Material.ARROW,
                builder);
        addDirectionButton(inv, 20, GradientDefinition.GradientDirection.HORIZONTAL_X, "X", Material.ARROW, builder);
        addDirectionButton(inv, 21, GradientDefinition.GradientDirection.HORIZONTAL_Z, "Z", Material.ARROW, builder);
        addDirectionButton(inv, 22, GradientDefinition.GradientDirection.RADIAL, "Radial", Material.TARGET, builder);

        addModeButton(inv, 27, GradientDefinition.InterpolationMode.LINEAR, "Linear", Material.IRON_INGOT, builder);
        addModeButton(inv, 28, GradientDefinition.InterpolationMode.SMOOTH, "Smooth", Material.GOLD_INGOT, builder);
        addModeButton(inv, 29, GradientDefinition.InterpolationMode.DISCRETE, "Discrete", Material.DIAMOND, builder);
        addModeButton(inv, 30, GradientDefinition.InterpolationMode.BLENDED, "Blended", Material.MOSSY_COBBLESTONE, builder);

        ItemStack typeToggle = new ItemStack(builder.useNoise ? Material.CHORUS_FRUIT : Material.PAPER);
        ItemMeta typeMeta = typeToggle.getItemMeta();
        typeMeta.displayName(Component.text(builder.useNoise ? "Noise Blend" : "Linear")
                .color(builder.useNoise ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD));
        typeMeta.lore(Arrays.asList(
                Component.text("Click to toggle").color(NamedTextColor.GRAY),
                Component.text(builder.useNoise ? "Using noise for variation" : "Clean gradient transition")
                        .color(NamedTextColor.DARK_GRAY)));
        typeToggle.setItemMeta(typeMeta);
        inv.setItem(31, typeToggle);

        if (builder.useNoise) {
            // Inline noise controls - Scale (slot 32)
            ItemStack scaleControl = new ItemStack(Material.AMETHYST_SHARD);
            ItemMeta scaleMeta = scaleControl.getItemMeta();
            scaleMeta.displayName(Component.text("Scale: " + String.format("%.2f", builder.noiseSettings.getScale()))
                    .color(NamedTextColor.AQUA));
            scaleMeta.lore(Arrays.asList(
                    Component.text("Left-click: Decrease").color(NamedTextColor.GRAY),
                    Component.text("Right-click: Increase").color(NamedTextColor.GRAY),
                    Component.text("Controls noise frequency").color(NamedTextColor.DARK_GRAY)));
            scaleControl.setItemMeta(scaleMeta);
            inv.setItem(32, scaleControl);

            // Inline noise controls - Strength (slot 33)
            ItemStack strengthControl = new ItemStack(Material.REDSTONE);
            ItemMeta strMeta = strengthControl.getItemMeta();
            strMeta.displayName(Component.text("Strength: " + String.format("%.2f", builder.noiseSettings.getStrength()))
                    .color(NamedTextColor.RED));
            strMeta.lore(Arrays.asList(
                    Component.text("Left-click: Decrease").color(NamedTextColor.GRAY),
                    Component.text("Right-click: Increase").color(NamedTextColor.GRAY),
                    Component.text("Controls noise intensity").color(NamedTextColor.DARK_GRAY)));
            strengthControl.setItemMeta(strMeta);
            inv.setItem(33, strengthControl);

            // Quick presets (slot 34)
            ItemStack presets = new ItemStack(Material.COMPARATOR);
            ItemMeta presetMeta = presets.getItemMeta();
            presetMeta.displayName(Component.text("Quick Presets").color(NamedTextColor.GOLD));
            presetMeta.lore(Arrays.asList(
                    Component.text("Left: Subtle").color(NamedTextColor.GREEN),
                    Component.text("Right: Strong").color(NamedTextColor.RED),
                    Component.text("Shift: Full Settings").color(NamedTextColor.YELLOW)));
            presets.setItemMeta(presetMeta);
            inv.setItem(34, presets);
        }

        if (builder.stops.size() >= 2) {
            previewRenderer.renderPreviewBarMultiBlock(inv, 36, builder.stops, builder.direction, builder.interpolationMode);
        } else {
            previewRenderer.renderEmptyPreview(inv, 36);
        }

        ItemStack worldPreview = new ItemStack(Material.ENDER_EYE);
        ItemMeta wpMeta = worldPreview.getItemMeta();
        wpMeta.displayName(Component.text("Preview in World").color(NamedTextColor.AQUA));
        wpMeta.lore(Arrays.asList(
                Component.text("Show gradient with particles").color(NamedTextColor.GRAY),
                Component.text("in your selection").color(NamedTextColor.GRAY)));
        worldPreview.setItemMeta(wpMeta);
        inv.setItem(45, worldPreview);

        ItemStack save = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text("Save as Preset").color(NamedTextColor.GOLD));
        saveMeta.lore(Arrays.asList(Component.text("Save this configuration").color(NamedTextColor.GRAY),
                Component.text("to your personal list").color(NamedTextColor.GRAY)));
        save.setItemMeta(saveMeta);
        inv.setItem(51, save);

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

    /**
     * Open the block selector for a specific stop.
     */
    private void openBlockSelector(Player player, int stopIndex) {
        player.getPersistentDataContainer().set(editingStopKey, PersistentDataType.INTEGER, stopIndex);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(BLOCK_SELECTOR_TITLE).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        List<Material> commonBlocks = Arrays.asList(
                Material.STONE, Material.COBBLESTONE, Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.SMOOTH_STONE,
                Material.SANDSTONE, Material.RED_SANDSTONE, Material.SAND, Material.RED_SAND,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE,
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE,
                Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE, Material.BROWN_CONCRETE,
                Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL,
                Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.GRAVEL,
                Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.BROWN_TERRACOTTA);

        for (int i = 0; i < Math.min(commonBlocks.size(), 45); i++) {
            ItemStack item = new ItemStack(commonBlocks.get(i));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(commonBlocks.get(i).name().toLowerCase().replace("_", " "))
                    .color(NamedTextColor.YELLOW));
            item.setItemMeta(meta);
            inv.setItem(9 + i, item);
        }

        player.openInventory(inv);
    }

    /**
     * Open noise settings configuration.
     */
    private void openNoiseSettings(Player player) {
        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(NOISE_SETTINGS_TITLE).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("<- Back").color(NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        addNoiseSlider(inv, 10, "Scale", builder.noiseSettings.getScale(), 0.01, 0.5);
        addNoiseSlider(inv, 19, "Strength", builder.noiseSettings.getStrength(), 0.0, 1.0);

        ItemStack algorithmToggle = new ItemStack(
                builder.noiseSettings.getAlgorithm() == NoiseSettings.NoiseAlgorithm.SIMPLEX
                        ? Material.AMETHYST_SHARD
                        : Material.FLINT);
        ItemMeta algoMeta = algorithmToggle.getItemMeta();
        algoMeta.displayName(Component.text("Algorithm: " + builder.noiseSettings.getAlgorithm().getDisplayName())
                .color(NamedTextColor.LIGHT_PURPLE));
        algoMeta.lore(Arrays.asList(
                Component.text("Click to toggle").color(NamedTextColor.GRAY),
                Component.text("Simplex: Smoother, organic").color(NamedTextColor.DARK_GRAY),
                Component.text("Perlin: Classic, grid-like").color(NamedTextColor.DARK_GRAY)));
        algorithmToggle.setItemMeta(algoMeta);
        inv.setItem(28, algorithmToggle);

        ItemStack randomSeed = new ItemStack(Material.SUNFLOWER);
        ItemMeta seedMeta = randomSeed.getItemMeta();
        seedMeta.displayName(Component.text("Randomize Seed").color(NamedTextColor.GOLD));
        seedMeta.lore(Arrays.asList(
                Component.text("Current: " + builder.noiseSettings.getSeed()).color(NamedTextColor.GRAY),
                Component.text("Click for new pattern").color(NamedTextColor.DARK_GRAY)));
        randomSeed.setItemMeta(seedMeta);
        inv.setItem(30, randomSeed);

        addNoisePresetButton(inv, 37, "Subtle", NoiseSettings.subtle(), builder);
        addNoisePresetButton(inv, 38, "Default", NoiseSettings.defaults(), builder);
        addNoisePresetButton(inv, 39, "Strong", NoiseSettings.strong(), builder);

        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text("Save Settings").color(NamedTextColor.GREEN));
        save.setItemMeta(saveMeta);
        inv.setItem(49, save);

        player.openInventory(inv);
    }

    private void addNoiseSlider(Inventory inv, int startSlot, String label, double value, double min, double max) {
        ItemStack decreaseFast = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta dfMeta = decreaseFast.getItemMeta();
        dfMeta.displayName(Component.text("<<").color(NamedTextColor.RED));
        decreaseFast.setItemMeta(dfMeta);
        inv.setItem(startSlot, decreaseFast);

        ItemStack decrease = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta dMeta = decrease.getItemMeta();
        dMeta.displayName(Component.text("<").color(NamedTextColor.GOLD));
        decrease.setItemMeta(dMeta);
        inv.setItem(startSlot + 1, decrease);

        ItemStack display = new ItemStack(Material.PAPER);
        ItemMeta displayMeta = display.getItemMeta();
        displayMeta
                .displayName(Component.text(label + ": " + String.format("%.2f", value)).color(NamedTextColor.WHITE));
        display.setItemMeta(displayMeta);
        inv.setItem(startSlot + 2, display);

        ItemStack increase = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta iMeta = increase.getItemMeta();
        iMeta.displayName(Component.text(">").color(NamedTextColor.GREEN));
        increase.setItemMeta(iMeta);
        inv.setItem(startSlot + 3, increase);

        ItemStack increaseFast = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta ifMeta = increaseFast.getItemMeta();
        ifMeta.displayName(Component.text(">>").color(NamedTextColor.DARK_GREEN));
        increaseFast.setItemMeta(ifMeta);
        inv.setItem(startSlot + 4, increaseFast);
    }

    private void addNoisePresetButton(Inventory inv, int slot, String name, NoiseSettings preset,
            AdvancedModeBuilder builder) {
        boolean isActive = Math.abs(builder.noiseSettings.getScale() - preset.getScale()) < 0.001
                && Math.abs(builder.noiseSettings.getStrength() - preset.getStrength()) < 0.001;

        ItemStack item = new ItemStack(isActive ? Material.GLOWSTONE_DUST : Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(isActive ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
        meta.lore(Arrays.asList(
                Component.text("Scale: " + String.format("%.2f", preset.getScale())).color(NamedTextColor.GRAY),
                Component.text("Strength: " + String.format("%.2f", preset.getStrength())).color(NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addDirectionButton(Inventory inv, int slot, GradientDefinition.GradientDirection direction,
            String name, Material material, AdvancedModeBuilder builder) {
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

    private void addModeButton(Inventory inv, int slot, GradientDefinition.InterpolationMode mode,
            String name, Material material, AdvancedModeBuilder builder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        boolean isSelected = builder.interpolationMode == mode;
        NamedTextColor color = isSelected ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        Component nameComp = Component.text(name).color(color);
        if (isSelected)
            nameComp = nameComp.decorate(TextDecoration.BOLD);
        meta.displayName(nameComp);

        List<Component> lore = new ArrayList<>();
        if (isSelected) {
            lore.add(Component.text("Selected").color(NamedTextColor.GREEN));
        }

        // Add mode descriptions
        switch (mode) {
            case LINEAR:
                lore.add(Component.text("Sharp transition at midpoint").color(NamedTextColor.GRAY));
                break;
            case SMOOTH:
                lore.add(Component.text("Smoothed transition curve").color(NamedTextColor.GRAY));
                break;
            case DISCRETE:
                lore.add(Component.text("Hard boundary, no blending").color(NamedTextColor.GRAY));
                break;
            case BLENDED:
                lore.add(Component.text("Organic mixed transitions").color(NamedTextColor.GRAY));
                lore.add(Component.text("Creates natural-looking gradients").color(NamedTextColor.DARK_GRAY));
                break;
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    /**
     * Handle block selection from the block browser.
     */
    private void handleBlockSelection(Player player, int stopIndex, List<BlockType> blocks) {
        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || blocks == null || blocks.isEmpty()) {
            openMainUI(player);
            return;
        }

        if (stopIndex < builder.stops.size()) {
            builder.stops.set(stopIndex, blocks);
        } else {
            builder.stops.add(blocks);
        }

        MessageManager.success(player, "Added stop with %d block(s).", blocks.size());
        openMainUI(player);
    }

    private void addSelectionStatus(Inventory inv, int slot, Player player) {
        boolean hasSelection = hasWorldEditSelection(player);
        ItemStack status = new ItemStack(hasSelection ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = status.getItemMeta();
        if (hasSelection) {
            meta.displayName(Component.text("Selection Ready").color(NamedTextColor.GREEN));
        } else {
            meta.displayName(Component.text("No Selection").color(NamedTextColor.RED));
            meta.lore(Arrays.asList(Component.text("Use WorldEdit to select").color(NamedTextColor.GRAY)));
        }
        status.setItemMeta(meta);
        inv.setItem(slot, status);
    }

    private boolean hasWorldEditSelection(Player player) {
        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            return WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Material getMaterialFromBlockType(BlockType blockType) {
        String materialName = blockType.id().replace("minecraft:", "").toUpperCase();
        Material matched = Material.matchMaterial(materialName);
        return matched != null ? matched : Material.STONE;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains(MAIN_TITLE)) {
            handleMainClick(event, player);
        } else if (title.contains(BLOCK_SELECTOR_TITLE)) {
            handleBlockSelectorClick(event, player);
        } else if (title.contains(NOISE_SETTINGS_TITLE)) {
            handleNoiseSettingsClick(event, player);
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();
        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        if (slot == 0) {
            player.closeInventory();
            // We should open Easy Mode here, but we need access to UIManager or EasyModeUI.
            // As a shortcut, we can use the command invocation or assume UIManager injected
            // eventually.
            // For now, let's just close and tell them.
            // Better: GradientUIManager should have passed itself or EasyModeUI to
            // AdvancedModeUI.
            // But I don't want to refactor constructor now.
            // Let's execute the command for easy mode.
            player.performCommand("gradient easy");
            return;
        }

        if (slot == 51) {
            // Save action
            builder.waitingForName = true;
            player.closeInventory();
            MessageManager.info(player, "Please type the name for this gradient in chat.");
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            activeBuilders.remove(player.getUniqueId());
            return;
        }

        if (slot >= 9 && slot < 18) {
            int stopIndex = slot - 9;
            if (event.getCurrentItem().getType() == Material.LIME_DYE) {
                // Adding new stop - open block browser
                int newStopIndex = builder.stops.size();
                if (event.isShiftClick()) {
                    // Shift+click for multi-select mode
                    blockBrowser.openMultiSelect(player, newStopIndex, (idx, blocks) -> {
                        handleBlockSelection(player, idx, blocks);
                    });
                } else {
                    // Normal click for single block
                    blockBrowser.openSingleSelect(player, newStopIndex, (idx, blocks) -> {
                        handleBlockSelection(player, idx, blocks);
                    });
                }
            } else if (event.isRightClick() && stopIndex < builder.stops.size()) {
                builder.stops.remove(stopIndex);
                openMainUI(player);
            } else if (stopIndex < builder.stops.size()) {
                // Editing existing stop - open block browser
                if (event.isShiftClick()) {
                    // Shift+click for multi-select mode
                    blockBrowser.openMultiSelect(player, stopIndex, (idx, blocks) -> {
                        handleBlockSelection(player, idx, blocks);
                    });
                } else {
                    // Normal click for single block
                    blockBrowser.openSingleSelect(player, stopIndex, (idx, blocks) -> {
                        handleBlockSelection(player, idx, blocks);
                    });
                }
            }
            return;
        }

        GradientDefinition.GradientDirection[] directions = GradientDefinition.GradientDirection.values();
        if (slot >= 18 && slot <= 22) {
            int dirIndex = slot - 18;
            if (dirIndex < directions.length) {
                builder.direction = directions[dirIndex];
                openMainUI(player);
            }
            return;
        }

        GradientDefinition.InterpolationMode[] modes = GradientDefinition.InterpolationMode.values();
        if (slot >= 27 && slot <= 30) {
            int modeIndex = slot - 27;
            if (modeIndex < modes.length) {
                builder.interpolationMode = modes[modeIndex];
                openMainUI(player);
            }
            return;
        }

        if (slot == 31) {
            builder.useNoise = !builder.useNoise;
            openMainUI(player);
            return;
        }

        // Inline noise controls
        if (builder.useNoise) {
            // Scale control (slot 32)
            if (slot == 32) {
                double val = builder.noiseSettings.getScale();
                val += event.isRightClick() ? 0.02 : -0.02;
                val = Math.max(0.01, Math.min(0.5, val));
                builder.noiseSettings = builder.noiseSettings.withScale(val);
                openMainUI(player);
                return;
            }

            // Strength control (slot 33)
            if (slot == 33) {
                double val = builder.noiseSettings.getStrength();
                val += event.isRightClick() ? 0.1 : -0.1;
                val = Math.max(0.0, Math.min(1.0, val));
                builder.noiseSettings = builder.noiseSettings.withStrength(val);
                openMainUI(player);
                return;
            }

            // Quick presets (slot 34)
            if (slot == 34) {
                if (event.isShiftClick()) {
                    openNoiseSettings(player);
                } else if (event.isRightClick()) {
                    builder.noiseSettings = NoiseSettings.strong();
                    openMainUI(player);
                } else {
                    builder.noiseSettings = NoiseSettings.subtle();
                    openMainUI(player);
                }
                return;
            }
        }

        if (slot == 45) {
            showWorldPreview(player, builder);
            return;
        }

        if (slot == 49) {
            applyGradient(player, builder);
        }
    }

    private nl.gzmn.gZMNBuildtools.gradient.BlockColorService blockColorService;

    public void setBlockColorService(nl.gzmn.gZMNBuildtools.gradient.BlockColorService blockColorService) {
        this.blockColorService = blockColorService;
    }

    private void handleBlockSelectorClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();

        // Handle clicks in player inventory (bottom inventory)
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            Material clickedMat = event.getCurrentItem().getType();
            // MessageManager.info(player, "DEBUG: Clicked inventory item: " + clickedMat);

            if (blockColorService == null) {
                MessageManager.error(player, "DEBUG: BlockColorService is null!");
                return;
            }

            if (clickedMat.isBlock() && !clickedMat.isAir()) {
                List<Material> suggestions = blockColorService.getGradientSuggestions(clickedMat, 3);
                if (suggestions.isEmpty()) {
                    MessageManager.warn(player, "No suggestions found for " + clickedMat);
                } else {
                    updateBlockSelectorWithSuggestions(event.getView().getTopInventory(), clickedMat, suggestions);
                    MessageManager.info(player, "Showing suggestions for %s", clickedMat.name());
                }
            } else {
                if (!clickedMat.isBlock())
                    MessageManager.info(player, "DEBUG: Not a block: " + clickedMat);
            }
            return;
        }

        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        if (slot == 0) {
            player.getPersistentDataContainer().remove(editingStopKey);
            openMainUI(player);
            return;
        }

        // Handle specific actions in the top inventory
        if (slot == 8) {
            // Reset to common blocks
            Integer stopIndex = player.getPersistentDataContainer().get(editingStopKey, PersistentDataType.INTEGER);
            if (stopIndex != null) {
                openBlockSelector(player, stopIndex);
            }
            return;
        }

        if (slot >= 9 && slot < 54) {
            // ... existing selection logic ...
            Material selectedMaterial = event.getCurrentItem().getType();
            if (selectedMaterial == Material.GRAY_STAINED_GLASS_PANE)
                return; // Placeholder

            BlockType blockType = BlockTypes.get("minecraft:" + selectedMaterial.name().toLowerCase());

            if (blockType != null) {
                Integer stopIndex = player.getPersistentDataContainer().get(editingStopKey, PersistentDataType.INTEGER);
                if (stopIndex != null) {
                    List<BlockType> singleBlockList = Arrays.asList(blockType);
                    if (stopIndex < builder.stops.size()) {
                        builder.stops.set(stopIndex, singleBlockList);
                    } else {
                        builder.stops.add(singleBlockList);
                    }
                    player.getPersistentDataContainer().remove(editingStopKey);
                }
            }
            openMainUI(player);
        }
    }

    private void updateBlockSelectorWithSuggestions(Inventory inv, Material origin, List<Material> suggestions) {
        // Clear previous common blocks area (9-53) except back button
        for (int i = 9; i < 54; i++) {
            inv.setItem(i, null);
        }

        // Title or info
        ItemStack originItem = new ItemStack(origin);
        ItemMeta originMeta = originItem.getItemMeta();
        originMeta.displayName(Component.text("Base: " + origin.name()).color(NamedTextColor.GOLD));
        originItem.setItemMeta(originMeta);
        inv.setItem(4, originItem); // Show what we are matching against at top

        // Display suggestions centered
        // We have up to 7 items? (3 darker + 1 origin + 3 lighter)
        // Let's display them in a row

        int startSlot = 19; // 2nd row center-ish

        // Add suggestions
        // suggestions is a list ordered darkest to lightest
        // Let's center them.
        startSlot = 22 - (suggestions.size() / 2);

        for (int i = 0; i < suggestions.size(); i++) {
            Material mat = suggestions.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(mat.name()).color(NamedTextColor.GREEN));
            if (mat == origin) {
                meta.lore(Arrays.asList(Component.text("Original Block").color(NamedTextColor.YELLOW)));
            }
            item.setItemMeta(meta);
            inv.setItem(startSlot + i, item);
        }

        // Add a "Show Common Blocks" button?
        ItemStack reset = new ItemStack(Material.BOOK);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.displayName(Component.text("Show Common Blocks").color(NamedTextColor.AQUA));
        reset.setItemMeta(resetMeta);
        inv.setItem(8, reset);
    }

    private void handleNoiseSettingsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();
        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null)
            return;

        if (slot == 0 || slot == 49) {
            openMainUI(player);
            return;
        }

        if (slot == 10)
            builder.noiseSettings = builder.noiseSettings.withScale(builder.noiseSettings.getScale() - 0.05);
        else if (slot == 11)
            builder.noiseSettings = builder.noiseSettings.withScale(builder.noiseSettings.getScale() - 0.01);
        else if (slot == 13)
            builder.noiseSettings = builder.noiseSettings.withScale(builder.noiseSettings.getScale() + 0.01);
        else if (slot == 14)
            builder.noiseSettings = builder.noiseSettings.withScale(builder.noiseSettings.getScale() + 0.05);
        else if (slot == 19)
            builder.noiseSettings = builder.noiseSettings.withStrength(builder.noiseSettings.getStrength() - 0.1);
        else if (slot == 20)
            builder.noiseSettings = builder.noiseSettings.withStrength(builder.noiseSettings.getStrength() - 0.05);
        else if (slot == 22)
            builder.noiseSettings = builder.noiseSettings.withStrength(builder.noiseSettings.getStrength() + 0.05);
        else if (slot == 23)
            builder.noiseSettings = builder.noiseSettings.withStrength(builder.noiseSettings.getStrength() + 0.1);
        else if (slot == 28) {
            NoiseSettings.NoiseAlgorithm newAlgo = builder.noiseSettings
                    .getAlgorithm() == NoiseSettings.NoiseAlgorithm.SIMPLEX
                            ? NoiseSettings.NoiseAlgorithm.PERLIN
                            : NoiseSettings.NoiseAlgorithm.SIMPLEX;
            builder.noiseSettings = builder.noiseSettings.withAlgorithm(newAlgo);
        } else if (slot == 30)
            builder.noiseSettings = builder.noiseSettings.withSeed(System.currentTimeMillis());
        else if (slot == 37)
            builder.noiseSettings = NoiseSettings.subtle();
        else if (slot == 38)
            builder.noiseSettings = NoiseSettings.defaults();
        else if (slot == 39)
            builder.noiseSettings = NoiseSettings.strong();

        openNoiseSettings(player);
    }

    private void showWorldPreview(Player player, AdvancedModeBuilder builder) {
        if (builder.stops.size() < 2) {
            MessageManager.error(player, "Add at least 2 gradient stops first.");
            return;
        }

        try {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(actor).getSelection(actor.getWorld());

            if (region == null) {
                MessageManager.error(player, "Selection required for preview.");
                return;
            }

            GradientDefinition gradient = builder.buildDefinition();
            GradientType gradientType = builder.useNoise ? new NoiseGradient() : new LinearGradient();
            GradientContext context = builder.useNoise
                    ? GradientContext.withNoise(builder.direction, builder.noiseSettings)
                    : GradientContext.linear(builder.direction);

            previewRenderer.showWorldPreview(player, region, gradient, gradientType, context);
            MessageManager.info(player, "Showing preview for 10 seconds...");

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required for preview.");
        }
    }

    private void applyGradient(Player player, AdvancedModeBuilder builder) {
        if (builder.stops.size() < 2) {
            MessageManager.error(player, "Add at least 2 gradient stops.");
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

            GradientDefinition gradient = builder.buildDefinition();
            GradientType gradientType = builder.useNoise ? new NoiseGradient() : new LinearGradient();
            GradientContext context = builder.useNoise
                    ? GradientContext.withNoise(builder.direction, builder.noiseSettings)
                    : GradientContext.linear(builder.direction);

            GradientExecutor.GradientResult result = GradientExecutor.getInstance().apply(
                    actor, region, gradient, gradientType, context);

            if (result.isSuccess()) {
                MessageManager.success(player, "Gradient applied to %d blocks.", result.getBlocksAffected());
            } else {
                MessageManager.error(player, "Failed: %s", result.getErrorMessage());
            }

            activeBuilders.remove(player.getUniqueId());

        } catch (IncompleteRegionException e) {
            MessageManager.error(player, "Selection required. Use WorldEdit to select an area.");
        } catch (Exception e) {
            MessageManager.error(player, "Error: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return;

        // Handle block input for gradient stops
        if (builder.waitingForBlocks) {
            event.setCancelled(true);
            String input = PlainTextComponentSerializer.plainText().serialize(event.message());

            Bukkit.getScheduler().runTask(plugin, () -> {
                builder.waitingForBlocks = false;

                // Parse block names (separated by |)
                String[] blockNames = input.split("\\|");
                List<BlockType> blockTypes = new ArrayList<>();

                for (String blockName : blockNames) {
                    blockName = blockName.trim();
                    if (blockName.isEmpty()) continue;

                    BlockType blockType = BlockTypes.get(blockName.contains(":") ? blockName : "minecraft:" + blockName);
                    if (blockType == null) {
                        MessageManager.error(player, "Unknown block: %s", blockName);
                        openMainUI(player);
                        return;
                    }
                    blockTypes.add(blockType);
                }

                if (blockTypes.isEmpty()) {
                    MessageManager.error(player, "No valid blocks specified.");
                    openMainUI(player);
                    return;
                }

                // Add or update the stop
                if (builder.editingStopIndex >= 0 && builder.editingStopIndex < builder.stops.size()) {
                    builder.stops.set(builder.editingStopIndex, blockTypes);
                } else {
                    builder.stops.add(blockTypes);
                }
                builder.editingStopIndex = -1;

                MessageManager.success(player, "Added stop with %d block(s).", blockTypes.size());
                openMainUI(player);
            });
            return;
        }

        // Handle name input for saving
        if (builder.waitingForName) {
            event.setCancelled(true);
            String name = PlainTextComponentSerializer.plainText().serialize(event.message());

            // Sync back to main thread for storage/UI operations
            Bukkit.getScheduler().runTask(plugin, () -> {
                builder.waitingForName = false;
                if (storageManager == null) {
                    MessageManager.error(player, "Storage manager not initialized.");
                    return;
                }

                // Create Preset from builder state
                GradientDefinition def = builder.buildDefinition();

                // Get block IDs (using primary block from each stop)
                List<String> blockIds = new ArrayList<>();
                for (List<BlockType> stopBlocks : builder.stops) {
                    // For saved presets, concatenate with | if multiple blocks
                    if (stopBlocks.size() == 1) {
                        blockIds.add(stopBlocks.get(0).toString());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < stopBlocks.size(); i++) {
                            if (i > 0) sb.append("|");
                            sb.append(stopBlocks.get(i).toString());
                        }
                        blockIds.add(sb.toString());
                    }
                }

                // Determine icon (first block of first stop)
                Material icon = Material.STONE;
                if (!builder.stops.isEmpty() && !builder.stops.get(0).isEmpty()) {
                    Material m = getMaterialFromBlockType(builder.stops.get(0).get(0));
                    if (m != null)
                        icon = m;
                }

                String id = "custom_" + UUID.randomUUID().toString().substring(0, 8);

                // Create GradientPreset
                GradientPreset preset = new GradientPreset.Builder(id)
                        .displayName(name)
                        .description("Custom gradient by " + player.getName())
                        .icon(icon)
                        .category(GradientPreset.PresetCategory.CUSTOM)
                        .blocks(blockIds)
                        .build();

                // Create SavedGradient
                SavedGradient saved = new SavedGradient(id, name, player.getUniqueId(), player.getName(), preset, false,
                        System.currentTimeMillis());

                storageManager.saveGradient(saved);
                MessageManager.success(player, "Gradient '%s' saved!", name);
                openMainUI(player);
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains(MAIN_TITLE) || title.contains(BLOCK_SELECTOR_TITLE)
                || title.contains(NOISE_SETTINGS_TITLE)) {
            Player player = (Player) event.getPlayer();

            // Don't remove builder if we are just closing to type name or blocks
            AdvancedModeBuilder builder = activeBuilders.get(player.getUniqueId());
            if (builder != null && (builder.waitingForName || builder.waitingForBlocks))
                return;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    AdvancedModeBuilder b = activeBuilders.get(player.getUniqueId());
                    // Double check waiting flags in case they were set after close
                    if (b != null && !b.waitingForName && !b.waitingForBlocks) {
                        activeBuilders.remove(player.getUniqueId());
                        player.getPersistentDataContainer().remove(editingStopKey);
                    }
                }
            }, 6000L);
        }
    }

    private static class AdvancedModeBuilder {
        List<List<BlockType>> stops = new ArrayList<>();  // Each stop can have multiple blocks
        GradientDefinition.GradientDirection direction = GradientDefinition.GradientDirection.VERTICAL_UP;
        GradientDefinition.InterpolationMode interpolationMode = GradientDefinition.InterpolationMode.LINEAR;
        boolean useNoise = false;
        NoiseSettings noiseSettings = NoiseSettings.defaults();
        boolean waitingForName = false;
        boolean waitingForBlocks = false;  // For chat input of multi-block stops
        int editingStopIndex = -1;  // Which stop index we're editing (-1 = adding new)

        GradientDefinition buildDefinition() {
            List<GradientDefinition.GradientStop> gradientStops = new ArrayList<>();
            for (int i = 0; i < stops.size(); i++) {
                double position = stops.size() > 1 ? (double) i / (stops.size() - 1) : 0.5;
                gradientStops.add(new GradientDefinition.GradientStop(stops.get(i), position));
            }
            return new GradientDefinition(gradientStops, direction, interpolationMode);
        }

        /**
         * Get the primary block type for a stop (for display purposes).
         */
        BlockType getPrimaryBlock(int stopIndex) {
            if (stopIndex >= 0 && stopIndex < stops.size() && !stops.get(stopIndex).isEmpty()) {
                return stops.get(stopIndex).get(0);
            }
            return null;
        }

        /**
         * Get a display string for a stop showing all blocks.
         */
        String getStopDisplayString(int stopIndex) {
            if (stopIndex >= 0 && stopIndex < stops.size()) {
                List<BlockType> blockTypes = stops.get(stopIndex);
                if (blockTypes.size() == 1) {
                    return blockTypes.get(0).id().replace("minecraft:", "");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < blockTypes.size(); i++) {
                        if (i > 0) sb.append("|");
                        sb.append(blockTypes.get(i).id().replace("minecraft:", ""));
                    }
                    return sb.toString();
                }
            }
            return "";
        }
    }
}
