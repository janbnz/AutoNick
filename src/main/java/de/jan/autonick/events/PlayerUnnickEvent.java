package de.jan.autonick.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerUnnickEvent extends PlayerEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final String nickname;

    public PlayerUnnickEvent(Player player, String nickname) {
        super(player);
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}