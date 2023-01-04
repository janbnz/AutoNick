package de.jan.autonick.database.databases;

import de.jan.autonick.database.Database;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySql extends Database {

    private final String host, database, username, password, tableName;
    private Connection connection;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public MySql(String host, String database, String username, String password, String tableName) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tableName = tableName;
    }

    public void connect() {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":3306/" + this.database + "?autoReconnect=true", this.username, this.password);
            this.update("CREATE TABLE IF NOT EXISTS " + this.tableName + "(UUID varchar(64), Activated int, NickName varchar(64));");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            this.connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setNickname(UUID uuid, String nickname) {
        this.update("UPDATE " + this.tableName + " SET NickName='" + nickname + "' WHERE UUID='" + uuid.toString() + "'");
    }

    @Override
    public boolean isExisting(UUID uuid) {
        final ResultSet resultSet = this.query("SELECT * FROM " + this.tableName + " WHERE UUID='" + uuid + "'");
        try {
            return resultSet.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasNickActivated(UUID uuid) {
        if (!this.isExisting(uuid)) return false;
        final ResultSet resultSet = this.query("SELECT * FROM " + this.tableName + " WHERE UUID='" + uuid + "'");
        try {
            return resultSet.getInt("Activated") == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void createPlayer(Player player) {
        this.update("INSERT INTO " + tableName + "(UUID, Activated, NickName) VALUES ('" + player
                .getUniqueId().toString() + "', '" + 0 + "', '" + player.getName() + "');");
    }

    @Override
    public void deletePlayer(UUID uuid) {
        this.update("DELETE FROM " + tableName + " WHERE UUID='" + uuid.toString() + "'");
    }

    @Override
    public void setNickState(UUID uuid, int state) {
        this.update("UPDATE " + tableName + " SET Activated='" + state + "' WHERE UUID='" + uuid.toString() + "'");
    }

    private void update(String sql) {
        this.executorService.execute(() -> {
            try {
                Statement statement = this.connection.createStatement();
                statement.executeUpdate(sql);
                statement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private ResultSet query(String sql) {
        ResultSet resultSet = null;
        try {
            Statement statement = this.connection.createStatement();
            resultSet = statement.executeQuery(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return resultSet;
    }

    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public String getTableName() {
        return this.tableName;
    }
}