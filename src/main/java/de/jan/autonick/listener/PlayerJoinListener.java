package de.jan.autonick.listener;

import de.jan.autonick.AutoNick;
import de.jan.autonick.AutoNickAPI;
import de.jan.autonick.database.DatabaseRegistry;
import de.jan.autonick.utils.ItemBuilder;
import de.jan.autonick.player.NickPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final AutoNickAPI api = AutoNick.getApi();

        if (player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
            if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")))
                                .setDisplayName(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.itemName")).build());
                    }
                }.runTaskLater(AutoNick.getInstance(), 5);
            }

            if (AutoNick.getConfiguration().getBoolean("nickOnThisServer")) {
                AutoNick.getApi().getPlayerInformation(player.getUniqueId(), new DatabaseRegistry.Callback() {
                    @Override
                    public void onSuccess(NickPlayer nickPlayer) {
                        if (nickPlayer.isNickActivated() || !AutoNick.getConfiguration().isBungeeCord()) {
                            api.nickPlayer(player);
                            player.sendMessage(AutoNick.getConfiguration().getString("nickItem.activatedItem.nickMessage").replace("{NICKNAME}", player.getCustomName()));
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.getInventory().setItem(AutoNick.getConfiguration().getInteger("NickItemSlot") - 1,
                                            new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
                                                    .setDisplayName(AutoNick.getConfiguration().getString("nickItem.activatedItem.itemName")).build());
                                }
                            }.runTaskLater(AutoNick.getInstance(), 5);
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                    }
                });
                return;
            }
        }

        if (AutoNick.getConfiguration().isBungeeCord()) {
            AutoNick.getApi().getPlayerInformation(player.getUniqueId(), new DatabaseRegistry.Callback() {
                @Override
                public void onSuccess(NickPlayer nickPlayer) {
                    if (nickPlayer.isExisting()) {
                        api.removeFromDatabase(player);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                }
            });
        }
    }
}