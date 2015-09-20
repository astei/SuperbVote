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
        player.sendMessage(message.replaceAll("%player%", vote.getName()).replaceAll("%service%", vote.getServiceName()));
    }

    @Override
    public void sendAsReminder(Player player) {
        int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());
        player.sendMessage(message.replaceAll("%player%", player.getName()).replaceAll("%votes%", Integer.toString(votes)));
    }
}
