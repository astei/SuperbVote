package io.minimum.minecraft.superbvote.handler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SuperbVoteHandler implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSuperbVote(SuperbVoteEvent event) {
        if (event.getVoteReward() == null) {
            throw new RuntimeException("No vote reward found for '" + event.getVote() + "'");
        }
        event.getVoteReward().runCommands(event.getVote());
    }
}
