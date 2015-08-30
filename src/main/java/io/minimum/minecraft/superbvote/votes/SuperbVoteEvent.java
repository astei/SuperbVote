package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SuperbVoteEvent extends Event {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final Vote vote;
    @Getter
    private VoteReward voteReward;

    public SuperbVoteEvent(Vote vote, VoteReward reward) {
        super();
        this.vote = vote;
        this.voteReward = reward;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
