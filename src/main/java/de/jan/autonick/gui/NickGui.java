package de.jan.autonick.gui;

import de.jan.autonick.database.DatabaseRegistry;
import de.jan.autonick.utils.ItemBuilder;
import de.jan.autonick.AutoNick;
import de.jan.autonick.player.NickPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NickGui {

    public static void open(final Player player) {
        final Inventory inventory = Bukkit.createInventory(null, 45, AutoNick.getConfiguration().getString("guiTitle"));

        final ItemStack glass = new ItemBuilder(Material.STAINED_GLASS_PANE, (byte) 15).setDisplayName(" ").build();
        for (int i = 0; i < 10; i++) inventory.setItem(i, glass);

        inventory.setItem(17, glass);
        inventory.setItem(18, glass);
        inventory.setItem(26, glass);
        inventory.setItem(27, glass);
        inventory.setItem(35, glass);

        for (int i = 36; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        AutoNick.getApi().getPlayerInformation(player.getUniqueId(), new DatabaseRegistry.Callback() {
            @Override
            public void onSuccess(NickPlayer nickPlayer) {
                if ((AutoNick.getConfiguration().isBungeeCord() && nickPlayer.isNickActivated()) || AutoNick.getApi().isNicked(player)) {
                    inventory.setItem(22,
                            new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
                                    .setDisplayName(AutoNick.getConfiguration().getString("nickItem.activatedItem.itemName")).build());
                } else {
                    inventory.setItem(22, new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")))
                            .setDisplayName(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.itemName")).build());
                }

                player.openInventory(inventory);
            }

            @Override
            public void onFailure(Exception exception) {
            }
        });
    }

}