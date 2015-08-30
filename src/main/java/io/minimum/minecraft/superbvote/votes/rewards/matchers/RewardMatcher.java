package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.votes.Vote;

public interface RewardMatcher {
    boolean matches(Vote vote);
}
