package de.jan.autonick.listener;

import de.jan.autonick.AutoNick;
import de.jan.autonick.AutoNickAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    @EventHandler
    private void onInventoryClick(final InventoryClickEvent event) {
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;
        if (!event.getView().getTitle().equals(AutoNick.getConfiguration().getString("guiTitle"))) return;
        if (event.getSlot() != 22) return;

        final Player player = (Player) event.getWhoClicked();

        final AutoNickAPI api = AutoNick.getApi();
        event.setCancelled(true);

        player.closeInventory();
        api.onPlayerInteract(player);
    }
}