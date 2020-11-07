/*
 * (C) Copyright 2020, Jan Benz, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.gui;

import de.seltrox.autonick.AutoNick;
import de.seltrox.autonick.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NickGui {

    public static void open(final Player player) {
        final Inventory inventory = Bukkit.createInventory(null, 45, AutoNick.getConfiguration().getString("guiTitle"));

        final ItemStack glass = new ItemBuilder(Material.STAINED_GLASS_PANE, (byte) 15)
                .setDisplayName(" ").build();
        for (int i = 0; i < 10; i++) {
            inventory.setItem(i, glass);
        }
        inventory.setItem(17, glass);
        inventory.setItem(18, glass);
        inventory.setItem(26, glass);
        inventory.setItem(27, glass);
        inventory.setItem(35, glass);
        for (int i = 36; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        if ((AutoNick.getConfiguration().isBungeeCord() && AutoNick.getApi().hasNickActivated(player.getUniqueId().toString())) || AutoNick.getApi().isNicked(player)) {
            inventory.setItem(22,
                    new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                            .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated")).build());
        } else {
            inventory.setItem(22, new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                    .setDisplayName(AutoNick.getConfiguration().getString("ItemNameDeactivated")).build());
        }

        player.openInventory(inventory);
    }

}