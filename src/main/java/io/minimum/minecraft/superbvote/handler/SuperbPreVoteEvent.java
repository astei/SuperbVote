package io.minimum.minecraft.superbvote.handler;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SuperbPreVoteEvent extends Event implements Cancellable {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    @Setter
    private boolean cancelled = false;
    @Getter
    private final Vote vote;

    public SuperbPreVoteEvent(Vote vote) {
        super(true);
        this.vote = vote;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
