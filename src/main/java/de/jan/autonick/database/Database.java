package de.jan.autonick.database;

import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class Database {

    public abstract void connect();

    public abstract void disconnect();

    public abstract void createPlayer(Player player);

    public abstract void deletePlayer(UUID uuid);

    public abstract void setNickname(UUID uuid, String nickname);

    public abstract boolean hasNickActivated(UUID uuid);

    public abstract boolean isExisting(UUID uuid);

    public abstract void setNickState(UUID uuid, int state);

    public abstract boolean isConnected();
}