package de.seltrox.autonick.listener;

import de.seltrox.autonick.AutoNick;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (AutoNick.getConfiguration().getBoolean("changeChat")) {
            if (AutoNick.getApi().isNicked(player)) {
                event.setFormat(AutoNick.getConfiguration().getString("chatFormat").replace("{NICKNAME}", AutoNick.getApi().getNickname(player)) + event.getMessage().replace("%", "%%"));
            }
        }
    }

}