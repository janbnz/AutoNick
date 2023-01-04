package de.jan.autonick.listener;

import de.jan.autonick.AutoNick;
import de.jan.autonick.AutoNickAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        final AutoNickAPI api = AutoNick.getApi();

        if (!api.isNicked(player)) return;
        api.quit(player);
    }
}