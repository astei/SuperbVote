package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class ClipsPlaceholderProvider implements PlaceholderProvider {
    @Override
    public String apply(String message, MessageContext context) {
        return context.getPlayer()
                .filter(OfflinePlayer::isOnline)
                .map(player -> PlaceholderAPI.setPlaceholders(player.getPlayer(), message))
                .orElse(message);
    }

    @Override
    public boolean canUse() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
