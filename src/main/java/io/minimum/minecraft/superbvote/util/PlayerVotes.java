package io.minimum.minecraft.superbvote.util;

import lombok.Value;
import org.bukkit.Bukkit;

import java.util.UUID;

@Value
public class PlayerVotes {
    private final UUID uuid;
    private final String associatedUsername;
    private final int votes;
    private final Type type;

    public String getAssociatedUsername() {
        if (associatedUsername == null) {
            return Bukkit.getOfflinePlayer(uuid).getName();
        }
        return associatedUsername;
    }

    public enum Type {
        CURRENT,
        FUTURE
    }
}
