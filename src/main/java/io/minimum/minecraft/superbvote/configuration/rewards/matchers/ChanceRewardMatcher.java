package io.minimum.minecraft.superbvote.configuration.rewards.matchers;

import io.minimum.minecraft.superbvote.handler.Vote;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Random;

@RequiredArgsConstructor
@ToString(exclude = {"random"})
public class ChanceRewardMatcher implements RewardMatcher {
    private final int chance;
    private final Random random = new Random();

    @Override
    public boolean matches(Vote vote) {
        return random.nextInt(chance) == 0;
    }
}
