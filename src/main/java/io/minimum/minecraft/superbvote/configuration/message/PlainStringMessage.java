package io.minimum.minecraft.superbvote.configuration.message;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.ClipsPlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.SuperbVotePlaceholderProvider;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlainStringMessage implements VoteMessage {
    private static final List<PlaceholderProvider> PROVIDER_LIST = ImmutableList.of(new SuperbVotePlaceholderProvider(),
            new ClipsPlaceholderProvider());

    private final String message;

    public PlainStringMessage(String message) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void sendAsBroadcast(Player player, Vote vote) {
        player.sendMessage(getAsBroadcast(vote));
    }

    protected String getAsBroadcast(Vote vote) {
        Player onlineVoted = Bukkit.getPlayerExact(vote.getName());
        String replaced = message;
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse()) {
                replaced = provider.applyForBroadcast(onlineVoted, message, vote);
            }
        }
        return replaced;
    }

    @Override
    public void sendAsReminder(Player player) {
        player.sendMessage(getAsReminder(player));
    }

    protected String getAsReminder(Player player) {
        String replaced = message;
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse()) {
                replaced = provider.applyForReminder(player, replaced);
            }
        }
        return replaced;
    }
}
