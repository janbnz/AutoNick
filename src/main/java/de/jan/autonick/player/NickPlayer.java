package de.jan.autonick.player;

import java.util.UUID;

public class NickPlayer {

    private final UUID uuid;
    private final boolean existing, nickActivated;

    public NickPlayer(UUID uuid, boolean existing, boolean nickActivated) {
        this.uuid = uuid;
        this.existing = existing;
        this.nickActivated = nickActivated;
    }

    public boolean isExisting() {
        return existing;
    }

    public boolean isNickActivated() {
        return nickActivated;
    }

    public UUID getUuid() {
        return uuid;
    }
}