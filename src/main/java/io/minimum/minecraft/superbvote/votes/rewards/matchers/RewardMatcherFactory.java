package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public interface RewardMatcherFactory {
    Optional<RewardMatcher> create(ConfigurationSection section);
}
