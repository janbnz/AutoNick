package de.jan.autonick.listener;

import de.jan.autonick.AutoNick;
import de.jan.autonick.AutoNickAPI;
import de.jan.autonick.gui.NickGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getItem().getType() != Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")) &&
                event.getItem().getType() != Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
            return;

        final AutoNickAPI api = AutoNick.getApi();

        if (!player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
            player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
            return;
        }

        if (api.hasDelay(player)) {
            if (AutoNick.getConfiguration().getBoolean("nickItem.delayMessage.enabled"))
                player.sendMessage(AutoNick.getConfiguration().getString("nickItem.delayMessage.message"));
            return;
        }

        if (AutoNick.getConfiguration().getBoolean("nickGui")) {
            NickGui.open(player);
            player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 1, 1);
            return;
        }

        api.onPlayerInteract(player);
    }
}