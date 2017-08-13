package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VoteReminder implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("superbvote.notify") && !SuperbVote.getPlugin().getVoteStorage().hasVotedToday(player.getUniqueId())) {
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(player);
            }
        }
    }
}
