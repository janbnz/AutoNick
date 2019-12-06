/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.listener;

import de.seltrox.autonick.AutoNick;
import de.seltrox.autonick.AutoNickAPI;
import de.seltrox.autonick.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AutoNickAPI api = AutoNick.getApi();

        if (player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
            if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                        .setDisplayName(AutoNick.getConfiguration().getString("ItemNameDeactivated")).build());
            }
            if (AutoNick.getConfiguration().isBungeeCord()) {
                if (AutoNick.getConfiguration().getBoolean("nickOnThisServer")) {
                    if (AutoNick.getConfiguration().isBungeeCord() && api.hasNickActivated(player.getUniqueId().toString())) {
                        api.nickPlayer(player);
                        player.getInventory().setItem(AutoNick.getConfiguration().getInteger("NickItemSlot") - 1,
                                new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                                        .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated")).build());
                        player.sendMessage(AutoNick.getConfiguration().getString("NickMessage").replace("{NICKNAME}", player.getCustomName()));
                    }
                }
            }
        } else {
            if (AutoNick.getConfiguration().isBungeeCord()) {
                if (api.isPlayerExisting(player.getUniqueId().toString())) {
                    api.removeFromDatabase(player);
                }
            }
        }
    }

}