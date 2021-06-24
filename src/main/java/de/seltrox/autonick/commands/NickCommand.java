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
  public boolean onCommand(CommandSender commandSender, Command command, String label,
      String[] args) {
    if (commandSender instanceof Player) {
      Player player = (Player) commandSender;
      if (player.isOp()) {
        if (args.length == 1 && args[0].equalsIgnoreCase("update")) {
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
          AutoNickAPI api = AutoNick.getApi();
          if (api.hasDelay(player)) {
            if (AutoNick.getConfiguration().getBoolean("MessageOnDelay")) {
              player.sendMessage(AutoNick.getConfiguration().getString("DelayMessage"));
            }
            return false;
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
    return false;
  }
}