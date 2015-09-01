package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CumulativeVotesRewardMatcher implements RewardMatcher {
    private final int votes;

    @Override
    public boolean matches(Vote vote) {
        return SuperbVote.getPlugin().getVoteStorage().getVotes(vote.getUuid()) == votes;
    }
}
