package de.seltrox.autonick;

import de.seltrox.autonick.commands.NickCommand;
import de.seltrox.autonick.config.AutoNickConfiguration;
import de.seltrox.autonick.listener.InventoryClickListener;
import de.seltrox.autonick.listener.PlayerChatListener;
import de.seltrox.autonick.listener.PlayerInteractListener;
import de.seltrox.autonick.listener.PlayerJoinListener;
import de.seltrox.autonick.listener.PlayerQuitListener;
import de.seltrox.autonick.mysql.MySql;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoNick extends JavaPlugin {

    private static AutoNickConfiguration configuration;
    private static AutoNick instance;
    private static AutoNickAPI api;
    private MySql mySql;

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
            mySql = new MySql(configuration.getString("Host"), configuration.getString("Database"), configuration.getString("Username"), configuration.getString("Password"));
            if (mySql.isConnected()) {
                String tableName = configuration.getString("TableName");
                mySql.update("CREATE TABLE IF NOT EXISTS " + tableName
                    + "(UUID varchar(64), Activated int, NickName varchar(64));");
                Bukkit.getConsoleSender().sendMessage(
                    configuration.getPrefix() + "§aSuccessfully connected to the database!");
            } else {
                configuration.setBungeeCord(false);
                Bukkit.getConsoleSender().sendMessage(
                    configuration.getPrefix() + "§cCan't connect to the MySQL database!");
                Bukkit.getConsoleSender()
                    .sendMessage(configuration.getPrefix() + "§cBungeeCord is now disabled!");
            }
        }

        /*     METRICS     */
        final int pluginId = 8730;
        final Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("bungeecord",
            () -> String.valueOf(configuration.isBungeeCord())));
    }

    @Override
    public void onDisable() {

    }

    public static AutoNickConfiguration getConfiguration() {
        return configuration;
    }

    public MySql getMySql() {
        return mySql;
    }

    public static AutoNick getInstance() {
        return instance;
    }

    public static AutoNickAPI getApi() {
        return api;
    }
}