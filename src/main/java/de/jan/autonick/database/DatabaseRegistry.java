package de.jan.autonick.database;

import de.jan.autonick.database.databases.MongoDb;
import de.jan.autonick.AutoNick;
import de.jan.autonick.database.databases.MySql;
import de.jan.autonick.player.NickPlayer;

public class DatabaseRegistry {

    private static Database database;

    public static void registerDatabase() {
        final String type = AutoNick.getConfiguration().getString("database.type").toLowerCase();

        switch (type) {
            case "mysql":
                database = new MySql(AutoNick.getConfiguration().getString("mySql.host"),
                        AutoNick.getConfiguration().getString("mySql.database"), AutoNick.getConfiguration().getString("mySql.username"),
                        AutoNick.getConfiguration().getString("mySql.password"), AutoNick.getConfiguration().getString("mySql.tableName"));
                break;
            case "mongodb":
                database = new MongoDb(AutoNick.getConfiguration().getString("mongodb.host"),
                        AutoNick.getConfiguration().getString("mongodb.username"), AutoNick.getConfiguration().getString("mongodb.password"),
                        AutoNick.getConfiguration().getString("mongodb.connectionUrl"), AutoNick.getConfiguration().getString("mongodb.database"),
                        AutoNick.getConfiguration().getString("mongodb.collection"));
                break;
        }

        if (database == null) return;
        database.connect();
    }

    public static void unregisterDatabase() {
        if (database == null || !database.isConnected()) return;
        database.disconnect();
    }

    public static Database getDatabase() {
        return database;
    }

    public interface Callback {
        void onSuccess(NickPlayer nickPlayer);

        void onFailure(Exception exception);
    }
}