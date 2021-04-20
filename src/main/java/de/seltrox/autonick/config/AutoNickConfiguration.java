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
import org.bukkit.configuration.file.FileConfiguration;

public class AutoNickConfiguration {

    private final FileConfiguration configuration;

    private boolean bungeeCord;

    public AutoNickConfiguration(AutoNick plugin) {
        plugin.saveDefaultConfig();
        configuration = plugin.getConfig();
        bungeeCord = getBoolean("bungeecord");
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