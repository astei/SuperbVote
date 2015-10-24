package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class SuperbVoteEvent extends Event {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final Vote vote;
    @Getter
    private List<VoteReward> voteRewards;

    public SuperbVoteEvent(Vote vote, List<VoteReward> rewards) {
        super();
        this.vote = vote;
        this.voteRewards = rewards;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
