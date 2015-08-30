package io.minimum.minecraft.superbvote.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatcher;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatchers;
import io.minimum.minecraft.superbvote.storage.JsonVoteStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SuperbVoteConfiguration {
    private final ConfigurationSection configuration;
    @Getter
    private final List<VoteReward> rewards;

    private static final List<String> SUPPORTED_STORAGE = ImmutableList.of("json");

    public SuperbVoteConfiguration(ConfigurationSection section) {
        this.configuration = section;

        rewards = section.getList("rewards").stream()
                .filter(s -> s instanceof Map)
                .map(s -> {
                    Map<?, ?> map = (Map<?, ?>) s;
                    MemoryConfiguration c = new MemoryConfiguration();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey().equals("if") && entry.getValue() instanceof Map) {
                            c.createSection("if", (Map<?, ?>) entry.getValue());
                        } else {
                            c.set(entry.getKey().toString(), entry.getValue());
                        }
                    }
                    return c;
                })
                .map(this::deserializeReward)
                .collect(Collectors.toList());

        if (rewards.isEmpty()) {
            throw new RuntimeException("No rewards defined.");
        }
    }

    private VoteReward deserializeReward(ConfigurationSection section) {
        Preconditions.checkNotNull(section, "section is not valid; is the default service section missing?");

        String name = section.getName();

        List<String> commands = section.getStringList("commands");
        String broadcast = section.getString("broadcast-message");
        String playerMessage = section.getString("player-message");

        if (broadcast == null) {
            throw new RuntimeException("'broadcast-message' missing in section '" + name + "' (do you need to enable 'inherit-default'?)");
        }

        if (playerMessage == null) {
            throw new RuntimeException("'player-message' missing in section '" + name + "' (do you need to enable 'inherit-default'?)");
        }

        List<RewardMatcher> rewards = RewardMatchers.getMatchers(section.getConfigurationSection("if"));

        return new VoteReward(name, rewards, commands, playerMessage, broadcast);
    }

    public VoteReward getBestReward(Vote vote) {
        for (VoteReward reward : rewards) {
            boolean use = true;
            for (RewardMatcher matcher : reward.getRewardMatchers()) {
                if (!matcher.matches(vote)) {
                    use = false;
                    break;
                }
            }
            if (use) return reward;
        }

        return null;
    }

    public boolean requirePlayersOnline() {
        return configuration.getBoolean("require-online", false);
    }

    public static String replacePlaceholders(String text, Vote vote) {
        return text.replaceAll("%player%", vote.getName()).replaceAll("%service%", vote.getServiceName());
    }

    public VoteStorage initializeVoteStorage() throws IOException {
        String storage = configuration.getString("storage.database");
        if (!SUPPORTED_STORAGE.contains(storage)) {
            SuperbVote.getPlugin().getLogger().info("Storage method '" + storage + "' is not valid, using JSON storage.");
            storage = "json";
        }

        switch (storage) {
            case "json":
                String file = configuration.getString("storage.json.file");
                if (file == null) {
                    file = "votes.json";
                    SuperbVote.getPlugin().getLogger().info("No file found in configuration, using 'votes.json'.");
                }
                return new JsonVoteStorage(new File(SuperbVote.getPlugin().getDataFolder(), file));
        }

        return null;
    }
}
