package io.minimum.minecraft.superbvote.votes.rewards;

import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvote.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatcher;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class VoteReward {
    private final String serviceName;
    private final List<RewardMatcher> rewardMatchers;
    private final List<String> commands;
    private final VoteMessage playerMessage;
    private final VoteMessage broadcastMessage;
    private final boolean cascade;

    public void broadcastVote(Vote vote, boolean playerAnnounce, boolean broadcast) {
        Player onlinePlayer = Bukkit.getPlayer(vote.getUuid());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerMessage != null && player == onlinePlayer && playerAnnounce) {
                playerMessage.sendAsBroadcast(player, vote);
            }
	    if (broadcastMessage != null && broadcast) {
                broadcastMessage.sendAsBroadcast(player, vote);
            }
        }
    }

    public void runCommands(Vote vote) {
        for (String command : commands) {
            String fixed = SuperbVoteConfiguration.replacePlaceholders(command, vote);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fixed);
        }
    }
}
