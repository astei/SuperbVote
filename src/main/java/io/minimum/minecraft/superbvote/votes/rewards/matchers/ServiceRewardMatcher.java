package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class ServiceRewardMatcher implements RewardMatcher {
    private final String name;

    @Override
    public boolean matches(Vote vote) {
        return vote.getServiceName().equals(name);
    }
}
