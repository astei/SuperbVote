package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

public class ClipsPlaceholderProvider implements PlaceholderProvider {
    @Override
    public String apply(String message, MessageContext context) {
        if (!context.getPlayer().isOnline()) {
            return message; // fallthrough
        }
        return PlaceholderAPI.setPlaceholders(context.getPlayer().getPlayer(), message);
    }

    @Override
    public boolean canUse() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
