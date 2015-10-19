package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SuperbVotePlaceholderProvider implements PlaceholderProvider {
    @Override
    public String applyForBroadcast(Player voted, String message, Vote vote) {
        return message.replaceAll("%player%", vote.getName()).replaceAll("%service%", vote.getServiceName());
    }

    @Override
    public String applyForReminder(Player player, String message) {
        int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());
        return message.replaceAll("%player%", player.getName()).replaceAll("%votes%", Integer.toString(votes));
    }

    @Override
    public boolean canUse() {
        return true; // Only depends on SuperbVote components.
    }

    @Override
    public boolean canUseForOfflinePlayers() {
        return true;
    }

    @Override
    public String applyForReminder(UUID player, String message) {
        int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(player);
        String name = SuperbVote.getPlugin().getUuidCache().getNameFromUuid(player);
        if (name != null)
            message = message.replaceAll("%player%", name);
        return message.replaceAll("%votes%", Integer.toString(votes));
    }
}
