package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlainStringMessage implements VoteMessage {
    private final String message;

    public PlainStringMessage(String message) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void sendAsBroadcast(Player player, Vote vote) {
        player.sendMessage(getAsBroadcast(player, vote));
    }

    protected String getAsBroadcast(Player player, Vote vote) {
        return message.replaceAll("%player%", vote.getName()).replaceAll("%service%", vote.getServiceName());
    }

    @Override
    public void sendAsReminder(Player player) {
        player.sendMessage(getAsReminder(player));
    }

    protected String getAsReminder(Player player) {
        int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());
        return message.replaceAll("%player%", player.getName()).replaceAll("%votes%", Integer.toString(votes));
    }
}
