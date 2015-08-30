package io.minimum.minecraft.superbvote.handler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SuperbVoteHandler implements Listener {
    @EventHandler
    public void onSuperbVote(SuperbVoteEvent event) {
        event.getVote().getService().broadcastVote(event.getVote());
        event.getVote().getService().runCommands(event.getVote());
    }
}
