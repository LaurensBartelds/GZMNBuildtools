package nl.gzmn.gZMNBuildtools.ui.typereplace;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

/**
 * Controller / listener for the Type Replace GUI. Coordinates the
 * {@link TypeReplaceMenu} view and {@link TypeReplaceSessions} state; holds no
 * rendering or catalogue logic itself.
 */
public class TypeReplaceUIManager implements Listener {

    private final Plugin plugin;
    private final TypeReplaceCommand typeReplaceCommand;
    private final TypeReplaceMenu menu = new TypeReplaceMenu();
    private final TypeReplaceSessions sessions = new TypeReplaceSessions();
    private Messages messages = AdventureMessages.basic();

    public TypeReplaceUIManager(Plugin plugin, TypeReplaceCommand typeReplaceCommand) {
        this.plugin = plugin;
        this.typeReplaceCommand = typeReplaceCommand;
    }

    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    public void setBlockFamilies(BlockFamilyRegistry blockFamilies) {
        this.menu.setBlockFamilies(blockFamilies);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTypeReplaceUI(Player player) {
        player.openInventory(menu.mainMenu(player, sessions.getOrCreate(player.getUniqueId())));
    }

    private void openCategorySelector(Player player, String selecting) {
        sessions.getOrCreate(player.getUniqueId()).selecting = selecting;
        player.openInventory(menu.categorySelector(selecting));
    }

    private void openMaterialSelector(Player player, TypeReplaceMenu.MaterialCategory category) {
        TypeReplaceSessions.Session session = sessions.getOrCreate(player.getUniqueId());
        session.categoryOrdinal = category.ordinal();
        player.openInventory(menu.materialSelector(category, session.selecting));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

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

        if (event.getCurrentItem() == null) {
            return;
        }

        TypeReplaceSessions.Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        switch (event.getSlot()) {
            case 37 -> openCategorySelector(player, "source");
            case 43 -> openCategorySelector(player, "target");
            case 49 -> executeReplacement(player, session);
            case 53 -> player.closeInventory();
        }
    }

    private void handleCategoryPageClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 0) {
            openTypeReplaceUI(player);
            return;
        }

        if (slot >= 9 && slot < 9 + menu.categoryCount()) {
            TypeReplaceMenu.MaterialCategory category = menu.categoryAt(slot - 9);
            if (category != null) {
                openMaterialSelector(player, category);
            }
        }
    }

    private void handleMaterialPageClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getSlot();
        TypeReplaceSessions.Session session = sessions.get(player.getUniqueId());

        if (slot == 0 || slot == 53) {
            String selecting = session != null ? session.selecting : null;
            if (selecting != null) {
                openCategorySelector(player, selecting);
            } else {
                openTypeReplaceUI(player);
            }
            return;
        }

        if (slot >= 9 && slot <= 44) {
            Integer categoryOrdinal = session != null ? session.categoryOrdinal : null;
            if (categoryOrdinal == null) {
                return;
            }

            TypeReplaceMenu.MaterialCategory category = menu.categoryAt(categoryOrdinal);
            if (category == null) {
                return;
            }

            List<String> materials = menu.materialsFor(category);
            int materialIndex = slot - 9;
            if (materialIndex < materials.size()) {
                setMaterialSelection(player, materials.get(materialIndex));
                openTypeReplaceUI(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!isOurMenu(title)) {
            return;
        }
        UUID id = player.getUniqueId();
        // A close also fires when navigating between our menus; only drop the
        // session if, a tick later, the player is no longer in any of our menus.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String openTitle = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
            if (!isOurMenu(openTitle)) {
                sessions.remove(id);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private static boolean isOurMenu(String title) {
        return title.contains("Type Replace") || title.contains("Category") || title.contains("Material");
    }

    private void setMaterialSelection(Player player, String material) {
        TypeReplaceSessions.Session session = sessions.get(player.getUniqueId());
        if (session == null || session.selecting == null) {
            return;
        }

        if (session.selecting.equals("source")) {
            session.sourceMaterial = material;
        } else {
            session.targetMaterial = material;
        }
    }

    private void executeReplacement(Player player, TypeReplaceSessions.Session session) {
        if (session.sourceMaterial == null) {
            messages.error(player, "Please select a source material.");
            return;
        }
        if (session.targetMaterial == null) {
            messages.error(player, "Please select a target material.");
            return;
        }

        player.closeInventory();

        typeReplaceCommand.execute(player, session.sourceMaterial, session.targetMaterial);

        sessions.remove(player.getUniqueId());
    }
}
