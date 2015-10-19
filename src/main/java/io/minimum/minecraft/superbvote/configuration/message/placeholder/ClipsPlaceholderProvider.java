package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.votes.Vote;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClipsPlaceholderProvider implements PlaceholderProvider {
    @Override
    public String applyForBroadcast(Player voted, String message, Vote vote) {
        if (voted == null) return message;
        return PlaceholderAPI.setPlaceholders(voted, message);
    }

    @Override
    public String applyForReminder(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    @Override
    public boolean canUse() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Override
    public boolean canUseForOfflinePlayers() {
        return false;
    }

    @Override
    public String applyForReminder(UUID player, String message) {
        return null;
    }
}
