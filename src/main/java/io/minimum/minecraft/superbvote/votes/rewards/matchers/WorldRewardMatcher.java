package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ToString
public class WorldRewardMatcher implements RewardMatcher {
    static RewardMatcherFactory FACTORY = section -> {
        if (section.isString("world")) {
            return Optional.of(new WorldRewardMatcher(ImmutableList.of(section.getString("world"))));
        } else if (section.isList("worlds")) {
            return Optional.of(new WorldRewardMatcher(section.getStringList("worlds")));
        }
        return Optional.empty();
    };

    private final List<String> names;

    public WorldRewardMatcher(List<String> names) {
        this.names = new ArrayList<>(names);
        this.names.replaceAll(String::toLowerCase);
    }

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        if (vote.getWorldName() == null)
            return false;
        return names.contains(vote.getWorldName().toLowerCase());
    }
}
