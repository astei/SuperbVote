package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class ChanceFractionalRewardMatcher implements RewardMatcher {
    private static final Random random = new Random();
    private final int chance;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        return random.nextInt(chance) == 0;
    }
}
