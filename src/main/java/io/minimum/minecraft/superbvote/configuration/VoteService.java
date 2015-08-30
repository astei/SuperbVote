package io.minimum.minecraft.superbvote.configuration;

import io.minimum.minecraft.superbvote.handler.Vote;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class VoteService {
    private final String serviceName;
    private transient final List<String> commands;
    private transient final String playerMessage;
    private transient final String broadcastMessage;

    public void broadcastVote(Vote vote) {
        Player onlinePlayer = Bukkit.getPlayer(vote.getUuid());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == onlinePlayer) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        SuperbVoteConfiguration.replacePlaceholders(playerMessage, vote)));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        SuperbVoteConfiguration.replacePlaceholders(broadcastMessage, vote)));
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
