package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class WorldRewardMatcher implements RewardMatcher {
    private final List<String> names;

    public WorldRewardMatcher(List<String> names) {
        this.names = new ArrayList<>(names);
        this.names.replaceAll(String::toLowerCase);
    }

    @Override
    public boolean matches(Vote vote) {
        if (vote.getWorldName() == null)
            return false;
        return names.contains(vote.getWorldName().toLowerCase());
    }
}
