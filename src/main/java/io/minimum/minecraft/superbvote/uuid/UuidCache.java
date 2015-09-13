package io.minimum.minecraft.superbvote.uuid;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UuidCache {
    public UUID getUuidFromName(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (player == null) return null;
        return player.getUniqueId();
    }

    public String getNameFromUuid(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player == null) return null;
        return player.getName();
    }
}
