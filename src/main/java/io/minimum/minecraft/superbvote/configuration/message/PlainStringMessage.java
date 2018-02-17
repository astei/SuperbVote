package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlainStringMessage extends MessageBase implements VoteMessage, OfflineVoteMessage {

    private final String message;

    public PlainStringMessage(String message) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void sendAsBroadcast(Player player, MessageContext context) {
        player.sendMessage(getAsBroadcast(message, context));
    }

    @Override
    public void sendAsReminder(Player player, MessageContext context) {
        player.sendMessage(getAsReminder(message, context));
    }

    @Override
    public void sendWithNothing(CommandSender to) {
        to.sendMessage(message);
    }

    @Override
    public String getWithOfflinePlayer(CommandSender to, MessageContext context) {
        String replaced = message;
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse() && provider.canUseForOfflinePlayers()) {
                replaced = provider.apply(replaced, context);
            }
        }
        return replaced;
    }
}
