package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.Random;

@RequiredArgsConstructor
public class ChancePercentageRewardMatcher implements RewardMatcher {
    static RewardMatcherFactory FACTORY = section -> {
        if (section.isInt("chance-percentage")) {
            int percentage = section.getInt("chance-percentage");
            Preconditions.checkArgument(percentage >= 1 && percentage < 100, "Chance percentage must be between 1 and 99.");
            return Optional.of(new ChancePercentageRewardMatcher(percentage));
        }
        return Optional.empty();
    };

    private static final Random random = new Random();
    private final int chance;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        return random.nextInt(100) < chance;
    }
}
