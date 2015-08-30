package io.minimum.minecraft.superbvote.handler;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SuperbVoteEvent extends Event {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final Vote vote;

    public SuperbVoteEvent(Vote vote) {
        super();
        this.vote = vote;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
