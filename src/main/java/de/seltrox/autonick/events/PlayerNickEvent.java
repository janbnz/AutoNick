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
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerNickEvent extends PlayerEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final String nickname;

    public PlayerNickEvent(Player player, String nickname) {
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