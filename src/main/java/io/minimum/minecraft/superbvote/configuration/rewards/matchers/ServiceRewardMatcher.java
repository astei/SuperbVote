package io.minimum.minecraft.superbvote.configuration.rewards.matchers;

import io.minimum.minecraft.superbvote.handler.Vote;
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
