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
import de.seltrox.autonick.gui.NickGui;
import de.seltrox.autonick.utils.ItemBuilder;
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
        if (event.getItem() != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                if (event.getItem().getType() == Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")) ||
                        event.getItem().getType() == Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated"))) {
                    AutoNickAPI api = AutoNick.getApi();
                    if (!player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
                        player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
                        return;
                    }

                    if (api.hasDelay(player)) {
                        if (AutoNick.getConfiguration().getBoolean("MessageOnDelay")) {
                            player.sendMessage(AutoNick.getConfiguration().getString("DelayMessage"));
                        }
                        return;
                    }

                    if (AutoNick.getConfiguration().getBoolean("nickGui")) {
                        NickGui.open(player);
                        player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 1, 1);
                    } else {
                        if (AutoNick.getConfiguration().isBungeeCord()) {
                            api.toggleNick(player);
                        } else {
                            if (api.isNicked(player)) {
                                player.sendMessage(AutoNick.getConfiguration().getString("UnnickMessage").replace("{NICKNAME}", player.getCustomName()));
                                api.unnick(player);
                                if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                                            .setDisplayName(AutoNick.getConfiguration().getString("ItemNameDeactivated")).build());
                                }
                            } else {
                                api.nickPlayer(player);
                                if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                                            .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated")).build());
                                }
                                player.sendMessage(AutoNick.getConfiguration().getString("NickMessage").replace("{NICKNAME}", AutoNick.getApi().getNickname(player)));
                            }
                        }
                    }
                }
            }
        }
    }

}