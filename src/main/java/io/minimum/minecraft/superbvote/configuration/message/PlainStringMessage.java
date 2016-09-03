package io.minimum.minecraft.superbvote.configuration.message;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.votes.Vote;

public class PlainStringMessage extends MessageBase implements VoteMessage, OfflineVoteMessage {

    private final String message;

    public PlainStringMessage(String message) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void sendAsBroadcast(Player player, Vote vote) {
        player.sendMessage(getAsBroadcast(message, vote));
    }

    @Override
    public void sendAsReminder(Player player) {
        player.sendMessage(getAsReminder(message, player));
    }

    @Override
    public void sendWithNothing(CommandSender to) {
        to.sendMessage(message);
    }

    @Override
    public String getWithOfflinePlayer(CommandSender to, UUID referred) {
        String replaced = message;
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse() && provider.canUseForOfflinePlayers()) {
                replaced = provider.applyForReminder(referred, replaced);
            }
        }
        return replaced;
    }
}
