package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChanceRewardMatcher implements RewardMatcher {
    private final int chance;

    @Override
    public boolean matches(Vote vote) {
        return vote.getDeterministicGenerator().nextInt(chance) == 0;
    }
}
