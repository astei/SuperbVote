package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;
import java.util.Random;

@RequiredArgsConstructor
public class ChanceFractionalRewardMatcher implements RewardMatcher {
    static RewardMatcherFactory FACTORY = section -> {
        if (section.isInt("chance-fractional")) {
            int denominator = section.getInt("chance-fractional");
            Preconditions.checkArgument(denominator > 1, "Chance denominator is less than or equal to one.");
            return Optional.of(new ChanceFractionalRewardMatcher(denominator));
        }
        return Optional.empty();
    };

    private static final Random random = new Random();
    private final int chance;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        return random.nextInt(chance) == 0;
    }
}
