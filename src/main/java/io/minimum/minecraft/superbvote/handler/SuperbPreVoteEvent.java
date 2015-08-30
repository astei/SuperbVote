package io.minimum.minecraft.superbvote.handler;

import io.minimum.minecraft.superbvote.configuration.rewards.VoteReward;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SuperbPreVoteEvent extends Event {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    @Setter
    private Result result = Result.PROCESS_VOTE;
    @Getter
    @Setter
    private VoteReward voteReward;
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

    public enum Result {
        PROCESS_VOTE,
        QUEUE_VOTE,
        CANCEL
    }
}
