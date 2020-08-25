/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerNickEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final Player player;
    private final String nickname;

    public PlayerNickEvent(Player player, String nickname) {
        this.player = player;
        this.nickname = nickname;
    }

    public Player getPlayer() {
        return player;
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