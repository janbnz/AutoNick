/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.mysql;

import de.seltrox.autonick.AutoNick;
import de.seltrox.autonick.player.NickPlayer;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySql {

    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final String tableName;

    private Connection connection;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public MySql(String host, String database, String username, String password, String tableName) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tableName = tableName;
        this.connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":3306/" + this.database + "?autoReconnect=true", this.username, this.password);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void update(String sql) {
        executorService.execute(() -> {
            try {
                Statement statement = connection.createStatement();
                statement.executeUpdate(sql);
                statement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public ResultSet query(String sql) {
        ResultSet resultSet = null;
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return resultSet;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public String getTableName() {
        return tableName;
    }

    public interface Callback {
        void onSuccess(NickPlayer nickPlayer);

        void onFailure(Exception exception);
    }

}