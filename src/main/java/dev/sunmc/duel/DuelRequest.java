package dev.sunmc.duel;

import java.util.UUID;

public class DuelRequest {

    private final UUID sender;
    private final UUID target;
    private final String kitName;
    private final long expireTime;

    public DuelRequest(UUID sender, UUID target, String kitName, long timeoutSeconds) {
        this.sender = sender;
        this.target = target;
        this.kitName = kitName;
        this.expireTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public String getKitName() { return kitName; }
}
