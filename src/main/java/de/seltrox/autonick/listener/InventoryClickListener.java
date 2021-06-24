/*
 * (C) Copyright 2020, Jan Benz, http://seltrox.de.
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
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

  @EventHandler
  private void onInventoryClick(final InventoryClickEvent event) {
    if (event.getClickedInventory() != null && event.getCurrentItem() != null) {
      final Player player = (Player) event.getWhoClicked();
      if (event.getView().getTitle()
          .equals(AutoNick.getConfiguration().getString("guiTitle"))) {
        AutoNickAPI api = AutoNick.getApi();
        event.setCancelled(true);

        if (event.getSlot() == 22) {
          player.closeInventory();
          if (AutoNick.getConfiguration().isBungeeCord()) {
            api.toggleNick(player);
          } else {
            if (api.isNicked(player)) {
              player.sendMessage(
                  AutoNick.getConfiguration().getString("UnnickMessage")
                      .replace("{NICKNAME}", player.getCustomName()));
              api.unnick(player);
              if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                    new ItemBuilder(Material
                        .getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                        .setDisplayName(
                            AutoNick.getConfiguration().getString("ItemNameDeactivated")).build());
              }
            } else {
              api.nickPlayer(player);
              if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                    new ItemBuilder(Material
                        .getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                        .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated"))
                        .build());
              }
              player.sendMessage(AutoNick.getConfiguration().getString("NickMessage")
                  .replace("{NICKNAME}", AutoNick.getApi().getNickname(player)));
            }
          }
        }
      }
    }
  }

}