package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SuperbVoteHandler implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSuperbVote(SuperbVoteEvent event) {
        if (event.getVoteRewards().isEmpty()) {
            throw new RuntimeException("No vote reward found for '" + event.getVote() + "'");
        }
        event.getVoteRewards().forEach(reward -> reward.runCommands(event.getVote()));
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), SuperbVote.getPlugin().getScoreboardHandler()::doPopulate);
    }
}
