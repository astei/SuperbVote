package io.minimum.minecraft.superbvote.uuid;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface UuidCache {
    void cachePlayer(Player player);

    UUID getUuidFromName(String name);

    String getNameFromUuid(UUID uuid);
}
