package nl.gzmn.gZMNBuildtools.command.brush;

import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BrushUIManager implements Listener {

    private final JavaPlugin plugin;
    private final Messages messages;

    // Tracks players currently asked to input a code
    private final Map<UUID, BrushSession> activeSessions = new ConcurrentHashMap<>();

    private BrushCommand brushCommand;

    public BrushUIManager(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setBrushCommand(BrushCommand brushCommand) {
        this.brushCommand = brushCommand;
    }

    public void openMainGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Gradient Brush Setup");

        // Shape selector
        inv.setItem(11, createItem(Material.SLIME_BALL, ChatColor.GREEN + "Shape: Sphere", ChatColor.GRAY + "Click to set shape to Sphere"));
        inv.setItem(12, createItem(Material.END_ROD, ChatColor.AQUA + "Shape: Cylinder", ChatColor.GRAY + "Click to set shape to Cylinder"));

        // Radius selector (default 5)
        inv.setItem(14, createItem(Material.COMPASS, ChatColor.YELLOW + "Radius: 5", ChatColor.GRAY + "Left click +1", ChatColor.GRAY + "Right click -1"));

        // Apply button
        inv.setItem(16, createItem(Material.EMERALD_BLOCK, ChatColor.GOLD + "Set Pattern & Apply", ChatColor.GRAY + "Click to enter your web editor code"));

        player.openInventory(inv);

        // Initialize default session state if not present
        activeSessions.put(player.getUniqueId(), new BrushSession("sphere", 5));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Gradient Brush Setup")) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BrushSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 11: // Sphere
                session.shape = "sphere";
                messages.info(player, "Shape set to Sphere.");
                break;
            case 12: // Cylinder
                session.shape = "cyl";
                messages.info(player, "Shape set to Cylinder.");
                break;
            case 14: // Radius
                if (event.isLeftClick()) {
                    session.radius = Math.min(50, session.radius + 1);
                } else if (event.isRightClick()) {
                    session.radius = Math.max(1, session.radius - 1);
                }

                ItemMeta meta = clicked.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.YELLOW + "Radius: " + session.radius);
                    clicked.setItemMeta(meta);
                }
                break;
            case 16: // Apply Code
                player.closeInventory();
                session.waitingForCode = true;
                player.sendMessage(ChatColor.GOLD + "========================================");
                player.sendMessage(ChatColor.YELLOW + "Please paste your code (e.g. gzmn@...) into chat.");
                player.sendMessage(ChatColor.GRAY + "Type 'cancel' to abort.");
                player.sendMessage(ChatColor.GOLD + "========================================");
                break;
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BrushSession session = activeSessions.get(player.getUniqueId());

        if (session != null && session.waitingForCode) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                activeSessions.remove(player.getUniqueId());
                messages.info(player, "Brush setup cancelled.");
                return;
            }

            // Execute on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (brushCommand != null) {
                    brushCommand.executeCommand(player, session.shape, session.radius, message);
                }
                activeSessions.remove(player.getUniqueId());
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class BrushSession {
        String shape;
        int radius;
        boolean waitingForCode = false;

        BrushSession(String shape, int radius) {
            this.shape = shape;
            this.radius = radius;
        }
    }
}
