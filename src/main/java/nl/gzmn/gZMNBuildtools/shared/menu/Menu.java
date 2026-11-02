package nl.gzmn.gZMNBuildtools.shared.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A chest menu whose slots are buttons. Identified by its holder (not its
 * title), so renamed or translated titles cannot confuse it with other chests.
 */
public abstract class Menu implements InventoryHolder {

    protected final Player viewer;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    protected Menu(Player viewer, int rows, Component title) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Fills the inventory; called on open and after every change. */
    protected abstract void render();

    public void open() {
        refresh();
        viewer.openInventory(inventory);
    }

    public void refresh() {
        inventory.clear();
        handlers.clear();
        render();
    }

    protected void button(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) {
            handlers.put(slot, onClick);
        }
    }

    protected void item(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    void clickTop(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = handlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    /** A click in the player's own inventory while this menu is open; ignored by default. */
    protected void clickBottom(InventoryClickEvent event) {
    }
}
