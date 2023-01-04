package de.jan.autonick.config;

import de.jan.autonick.AutoNick;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class AutoNickConfiguration {

    private FileConfiguration configuration;

    private boolean bungeeCord;

    public AutoNickConfiguration(AutoNick plugin) {
        plugin.saveDefaultConfig();

        try {
            this.configuration = YamlConfiguration.loadConfiguration(
                    new BufferedReader(new InputStreamReader(new FileInputStream(new File(plugin.getDataFolder(), "config.yml")), StandardCharsets.UTF_8)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        }

        this.bungeeCord = this.getBoolean("bungeecord");
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
        return getInteger("nickItem.slot") - 1;
    }
}