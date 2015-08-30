package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VoteReminder implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int count = SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());
            String text = SuperbVote.getPlugin().getConfig().getString("vote-reminder.message");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    SuperbVoteConfiguration.replacePlaceholders(text, player.getName(), count)));
        }
    }
}
