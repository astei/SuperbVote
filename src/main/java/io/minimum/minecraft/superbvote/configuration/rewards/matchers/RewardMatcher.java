package io.minimum.minecraft.superbvote.configuration.rewards.matchers;

import io.minimum.minecraft.superbvote.handler.Vote;

public interface RewardMatcher {
    boolean matches(Vote vote);
}
