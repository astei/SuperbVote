package io.minimum.minecraft.superbvote.uuid;

import org.bukkit.Bukkit;

import java.util.UUID;

public class UuidCache {
    public UUID getUuidFromName(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    public String getNameFromUuid(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}
