package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CumulativeVotesRewardMatcher implements RewardMatcher {
    static RewardMatcherFactory FACTORY = section -> {
        if (section.isInt("cumulative-votes")) {
            int votes = section.getInt("cumulative-votes");
            Preconditions.checkArgument(votes >= 1, "cumulative-votes number must be greater than or equal to 1.");
            return Optional.of(new CumulativeVotesRewardMatcher(votes));
        }
        return Optional.empty();
    };

    private final int votes;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        int cur = pv.getType() == PlayerVotes.Type.FUTURE ? pv.getVotes() : pv.getVotes() + 1;
        return cur == votes;
    }
}
