package de.jan.autonick;

import de.jan.autonick.config.AutoNickConfiguration;
import de.jan.autonick.database.DatabaseRegistry;
import de.jan.autonick.commands.NickCommand;
import de.jan.autonick.listener.InventoryClickListener;
import de.jan.autonick.listener.PlayerChatListener;
import de.jan.autonick.listener.PlayerInteractListener;
import de.jan.autonick.listener.PlayerJoinListener;
import de.jan.autonick.listener.PlayerQuitListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoNick extends JavaPlugin {

    private static AutoNickConfiguration configuration;
    private static AutoNick instance;
    private static AutoNickAPI api;

    @Override
    public void onEnable() {
        /*      GENERAL      */
        instance = this;
        configuration = new AutoNickConfiguration(this);

        api = new AutoNickAPI();

        /*      LISTENER     */
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(), this);
        pluginManager.registerEvents(new PlayerInteractListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);
        pluginManager.registerEvents(new PlayerChatListener(), this);
        pluginManager.registerEvents(new InventoryClickListener(), this);

        /*      COMMANDS    */
        getCommand("nick").setExecutor(new NickCommand());

        /*      DATABASE    */
        if (configuration.isBungeeCord()) {
            DatabaseRegistry.registerDatabase();
            if (DatabaseRegistry.getDatabase().isConnected()) {
                Bukkit.getConsoleSender().sendMessage(configuration.getPrefix() + "§aSuccessfully connected to the database!");
            } else {
                configuration.setBungeeCord(false);
                Bukkit.getConsoleSender().sendMessage(configuration.getPrefix() + "§cCan't connect to the database!");
                Bukkit.getConsoleSender().sendMessage(configuration.getPrefix() + "§cBungeeCord is now disabled!");
            }
        }

        /*     METRICS     */
        final int pluginId = 8730;
        final Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("bungeecord", () -> String.valueOf(configuration.isBungeeCord())));
    }

    @Override
    public void onDisable() {
        DatabaseRegistry.unregisterDatabase();
    }

    public static AutoNickConfiguration getConfiguration() {
        return configuration;
    }

    public static AutoNick getInstance() {
        return instance;
    }

    public static AutoNickAPI getApi() {
        return api;
    }
}