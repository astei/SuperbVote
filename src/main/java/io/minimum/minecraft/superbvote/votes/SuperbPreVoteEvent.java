package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class SuperbPreVoteEvent extends Event {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    @Setter
    private Result result = Result.PROCESS_VOTE;
    @Getter
    private List<VoteReward> voteRewards;
    @Getter
    private final Vote vote;

    public SuperbPreVoteEvent(Vote vote, List<VoteReward> rewardList) {
        super(true);
        this.vote = vote;
        this.voteRewards = rewardList;
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
