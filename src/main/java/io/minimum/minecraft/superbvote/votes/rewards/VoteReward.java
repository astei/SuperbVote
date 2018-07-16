package io.minimum.minecraft.superbvote.votes.rewards;

import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatcher;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Data
public class VoteReward {
    private final String serviceName;
    private final List<RewardMatcher> rewardMatchers;
    private final List<String> commands;
    private final VoteMessage playerMessage;
    private final VoteMessage broadcastMessage;
    private final boolean cascade;

    public void broadcastVote(MessageContext context, boolean playerAnnounce, boolean broadcast) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerMessage != null && Optional.of(player).equals(context.getPlayer()) && playerAnnounce) {
                playerMessage.sendAsBroadcast(player, context);
            }
            if (broadcastMessage != null && broadcast) {
                broadcastMessage.sendAsBroadcast(player, context);
            }
        }
    }

    public void runCommands(Vote vote) {
        for (String command : commands) {
            String fixed = SuperbVoteConfiguration.replaceCommandPlaceholders(command, vote);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fixed);
        }
    }
}
