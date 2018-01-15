package io.minimum.minecraft.superbvote.configuration.message;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.ClipsPlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.SuperbVotePlaceholderProvider;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageBase {
    protected static final List<PlaceholderProvider> PROVIDER_LIST = ImmutableList.of(new SuperbVotePlaceholderProvider(),
            new ClipsPlaceholderProvider());

    protected String getAsBroadcast(String message, Vote vote) {
        Player onlineVoted = Bukkit.getPlayerExact(vote.getName());
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse()) {
                message = provider.applyForBroadcast(onlineVoted, message, vote);
            }
        }
        return message;
    }

    protected String getAsReminder(String message, Player player) {
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse()) {
                message = provider.applyForReminder(player, message);
            }
        }
        return message;
    }
}
