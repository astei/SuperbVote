package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SuperbVotePlaceholderProvider implements PlaceholderProvider {
    @Override
    public String applyForBroadcast(Player voted, String message, Vote vote) {
        return message.replace("%player%", vote.getName()).replace("%service%", vote.getServiceName())
                .replace("%uuid%", vote.getUuid().toString());
    }

    @Override
    public String applyForReminder(Player player, String message) {
        int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());
        return message.replace("%player%", player.getName()).replace("%votes%", Integer.toString(votes))
                .replace("%uuid%", player.getUniqueId().toString());
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
            message = message.replace("%player%", name);
        return message.replace("%votes%", Integer.toString(votes)).replace("%uuid%", player.toString());
    }
}
