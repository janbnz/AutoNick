/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.config;

import de.seltrox.autonick.AutoNick;
import jdk.internal.util.xml.impl.ReaderUTF8;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class AutoNickConfiguration {

    private FileConfiguration configuration;

    private boolean bungeeCord;

    public AutoNickConfiguration(AutoNick plugin) {
        plugin.saveDefaultConfig();
        try {
            configuration = YamlConfiguration.loadConfiguration(new ReaderUTF8(new FileInputStream(new File(plugin.getDataFolder(), "config.yml"))));
            bungeeCord = getBoolean("bungeecord");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        if (!key.equals("prefix")) {
            return configuration.getString(key).replace("&", "§").replace("{PREFIX}", getString("prefix"));
        }
        return configuration.getString(key).replace("&", "§");
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