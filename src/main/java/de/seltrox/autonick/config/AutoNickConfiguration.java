/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.config;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;
import de.seltrox.autonick.AutoNick;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class AutoNickConfiguration {

  private FileConfiguration configuration;

  private boolean bungeeCord;

  public AutoNickConfiguration(AutoNick plugin) {
    plugin.saveDefaultConfig();

    try {
      configuration = YamlConfiguration.loadConfiguration(
          new UTF8Reader(new FileInputStream(new File(plugin.getDataFolder(), "config.yml"))));
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      return;
    }

    bungeeCord = getBoolean("bungeecord");
  }

  public String getString(String key) {
    if (!key.equals("prefix")) {
      return ChatColor.translateAlternateColorCodes('&',
          configuration.getString(key).replace("{PREFIX}", getString("prefix")));
    }
    return ChatColor.translateAlternateColorCodes('&', configuration.getString(key));
  }

  public boolean getBoolean(String key) {
    return configuration.getBoolean(key);
  }

  public int getInteger(String key) {
    return configuration.getInt(key);
  }

  public String getPrefix() {
    return configuration.getString("prefix").replace("&", "§") + " ";
  }

  public boolean isBungeeCord() {
    return bungeeCord;
  }

  public void setBungeeCord(boolean bungeeCord) {
    this.bungeeCord = bungeeCord;
  }

  public int getNickItemSlot() {
    return getInteger("NickItemSlot") - 1;
  }
}