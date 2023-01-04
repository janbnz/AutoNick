package de.jan.autonick.commands;

import de.jan.autonick.AutoNick;
import de.jan.autonick.AutoNickAPI;
import de.jan.autonick.utils.ItemBuilder;
import de.jan.autonick.utils.SpigotUpdater;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) return false;
        final Player player = (Player) commandSender;

        if (!player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
            player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
            return false;
        }

        if (args.length != 1) {
            executeNickCommand(player, null);
            return false;
        }

        if (args[0].equalsIgnoreCase("update")) {
            if (!(player.hasPermission(AutoNick.getConfiguration().getString("adminPermission")))) {
                player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
                return false;
            }

            final SpigotUpdater spigotUpdater = new SpigotUpdater();
            if (spigotUpdater.isUpdateAvailable()) {
                player.sendMessage(AutoNick.getConfiguration().getPrefix() + "§7A new version of the plugin is available!");
                player.sendMessage(AutoNick.getConfiguration().getPrefix() + "§ahttps://www.spigotmc.org/resources/27441/");
            } else {
                player.sendMessage(AutoNick.getConfiguration().getPrefix() + "§7The plugin is up to date!");
            }
            return false;
        }

        if (!(AutoNick.getConfiguration().getBoolean("chooseOwnNick") && player
                .hasPermission(AutoNick.getConfiguration().getString("ownNickPermission")))) {
            executeNickCommand(player, null);
            return false;
        }

        if (AutoNick.getApi().isNicked(player)) {
            executeNickCommand(player, null);
            return false;
        }

        String nickname = args[0];
        if (nickname.length() > 16) {
            player.sendMessage(AutoNick.getConfiguration().getString("tooManyChars"));
            return false;
        }

        if (nickname.length() < 3) {
            player.sendMessage(AutoNick.getConfiguration().getString("tooShort"));
            return false;
        }

        if (nickname.equals(player.getName())) {
            player.sendMessage(AutoNick.getConfiguration().getString("choosedOwnName"));
            return false;
        }

        executeNickCommand(player, args[0]);
        return false;
    }

    private void executeNickCommand(Player player, String nickname) {
        final AutoNickAPI api = AutoNick.getApi();
        if (api.hasDelay(player)) {
            if (AutoNick.getConfiguration().getBoolean("nickItem.delayMessage.enabled"))
                player.sendMessage(AutoNick.getConfiguration().getString("nickItem.delayMessage.message"));
            return;
        }

        if (!AutoNick.getConfiguration().getBoolean("nickOnThisServer") && AutoNick.getConfiguration().isBungeeCord()) {
            api.toggleNick(player);
            return;
        }

        if (api.isNicked(player)) {
            player.sendMessage(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.unnickMessage")
                    .replace("{NICKNAME}", player.getCustomName()));
            api.unnick(player);

            if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                        new ItemBuilder(Material
                                .getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")))
                                .setDisplayName(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.itemName")).build());
            }
            return;
        }

        if (nickname == null) {
            api.nickPlayer(player);
        } else {
            api.nickPlayer(player, nickname);
        }

        if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
            player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                    new ItemBuilder(Material
                            .getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
                            .setDisplayName(AutoNick.getConfiguration().getString("nickItem.activatedItem.itemName")).build());
        }

        player.sendMessage(AutoNick.getConfiguration().getString("nickItem.activatedItem.nickMessage")
                .replace("{NICKNAME}", AutoNick.getApi().getNickname(player)));
    }
}