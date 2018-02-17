package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;

public interface RewardMatcher {
    boolean matches(Vote vote, PlayerVotes pv);
}
