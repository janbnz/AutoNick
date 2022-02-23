/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.commands;

import de.seltrox.autonick.AutoNick;
import de.seltrox.autonick.AutoNickAPI;
import de.seltrox.autonick.utils.ItemBuilder;
import de.seltrox.autonick.utils.SpigotUpdater;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (player.hasPermission(AutoNick.getConfiguration().getString("permission"))) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("update")) {
                        if (!(player.hasPermission(AutoNick.getConfiguration().getString("adminPermission")))) {
                            player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
                            return false;
                        }
                        SpigotUpdater spigotUpdater = new SpigotUpdater();
                        if (spigotUpdater.isUpdateAvailable()) {
                            player.sendMessage(AutoNick.getConfiguration().getPrefix()
                                    + "§7A new version of the plugin is available!");
                            player.sendMessage(AutoNick.getConfiguration().getPrefix()
                                    + "§7https://www.spigotmc.org/resources/27441/");
                        } else {
                            player.sendMessage(
                                    AutoNick.getConfiguration().getPrefix() + "§7The plugin is up to date!");
                        }
                    } else {
                        if (AutoNick.getConfiguration().getBoolean("chooseOwnNick") && player
                                .hasPermission(AutoNick.getConfiguration().getString("ownNickPermission"))) {

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
                        } else {
                            executeNickCommand(player, null);
                        }
                    }
                } else {
                    executeNickCommand(player, null);
                }
            } else {
                player.sendMessage(AutoNick.getConfiguration().getString("noPermission"));
            }
        }
        return false;
    }

    private void executeNickCommand(Player player, String nickname) {
        AutoNickAPI api = AutoNick.getApi();
        if (api.hasDelay(player)) {
            if (AutoNick.getConfiguration().getBoolean("MessageOnDelay")) {
                player.sendMessage(AutoNick.getConfiguration().getString("DelayMessage"));
            }
            return;
        }
        if (!AutoNick.getConfiguration().getBoolean("nickOnThisServer") && AutoNick
                .getConfiguration().isBungeeCord()) {
            api.toggleNick(player);
        } else {
            if (api.isNicked(player)) {
                player.sendMessage(AutoNick.getConfiguration().getString("UnnickMessage")
                        .replace("{NICKNAME}", player.getCustomName()));
                api.unnick(player);
                if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                            new ItemBuilder(Material
                                    .getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                                    .setDisplayName(
                                            AutoNick.getConfiguration().getString("ItemNameDeactivated"))
                                    .build());
                }
            } else {
                if (nickname == null) {
                    api.nickPlayer(player);
                } else {
                    api.nickPlayer(player, nickname);
                }
                if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                            new ItemBuilder(Material
                                    .getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                                    .setDisplayName(
                                            AutoNick.getConfiguration().getString("ItemNameActivated"))
                                    .build());
                }
                player.sendMessage(AutoNick.getConfiguration().getString("NickMessage")
                        .replace("{NICKNAME}", AutoNick.getApi().getNickname(player)));
            }
        }
    }
}