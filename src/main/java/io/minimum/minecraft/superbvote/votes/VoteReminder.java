package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VoteReminder implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(player);
        }
    }
}
