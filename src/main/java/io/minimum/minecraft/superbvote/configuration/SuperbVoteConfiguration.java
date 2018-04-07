package io.minimum.minecraft.superbvote.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.VoteCommand;
import io.minimum.minecraft.superbvote.configuration.message.OfflineVoteMessages;
import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import io.minimum.minecraft.superbvote.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvote.configuration.message.VoteMessages;
import io.minimum.minecraft.superbvote.storage.JsonVoteStorage;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.*;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuperbVoteConfiguration {
    private final ConfigurationSection configuration;
    @Getter
    private final List<VoteReward> rewards;
    @Getter
    private final VoteMessage reminderMessage;
    @Getter
    private final VoteCommand voteCommand;
    @Getter
    private final TextLeaderboardConfiguration textLeaderboardConfiguration;
    @Getter
    private final TopPlayerSignsConfiguration topPlayerSignsConfiguration;

    private static final List<String> SUPPORTED_STORAGE = ImmutableList.of("json", "mysql");

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

        long defaultRewardCount = rewards.stream()
                .filter(r -> r.getRewardMatchers().contains(StaticRewardMatcher.ALWAYS_MATCH) || r.getRewardMatchers().isEmpty())
                .count();
        if (defaultRewardCount == 0) {
            throw new RuntimeException("No default reward was defined. To set a default reward, set default: true in one of your reward if blocks.");
        }
        if (defaultRewardCount > 1) {
            throw new RuntimeException("Multiple default rewards are defined. Only one default reward can be used.");
        }

        reminderMessage = VoteMessages.from(configuration, "vote-reminder.message");

        if (configuration.getBoolean("vote-command.enabled")) {
            boolean useJson = configuration.getBoolean("vote-command.use-json-text");
            VoteMessage voteMessage = VoteMessages.from(configuration, "vote-command.text", false, useJson);
            voteCommand = new VoteCommand(voteMessage);
        } else {
            voteCommand = null;
        }

        textLeaderboardConfiguration = new TextLeaderboardConfiguration(
                configuration.getInt("leaderboard.text.per-page", 10),
                OfflineVoteMessages.from(configuration.getConfigurationSection("leaderboard.text"), "header"),
                OfflineVoteMessages.from(configuration.getConfigurationSection("leaderboard.text"), "entry")
        );

        topPlayerSignsConfiguration = new TopPlayerSignsConfiguration(
                configuration.getStringList("top-player-signs.format").stream()
                        .map(PlainStringMessage::new)
                        .collect(Collectors.toList())
        );
    }

    private VoteReward deserializeReward(ConfigurationSection section) {
        Preconditions.checkNotNull(section, "section is not valid");

        String name = section.getName();

        List<String> commands = section.getStringList("commands");
        VoteMessage broadcast = VoteMessages.from(section, "broadcast-message", true, false);
        VoteMessage playerMessage = VoteMessages.from(section, "player-message", true, false);

        List<RewardMatcher> rewards = RewardMatchers.getMatchers(section.getConfigurationSection("if"));
        boolean cascade = section.getBoolean("allow-cascading");

        return new VoteReward(name, rewards, commands, playerMessage, broadcast, cascade);
    }

    public List<VoteReward> getBestRewards(Vote vote, PlayerVotes pv) {
        List<VoteReward> best = new ArrayList<>();
        // We only allow random chances to match just once when cascading, the player should not get another chance
        // to "win".
        boolean chanceMatched = false;
        rewardCheck:
        for (VoteReward reward : rewards) {
            boolean rewardMatchChance = false;
            for (RewardMatcher matcher : reward.getRewardMatchers()) {
                boolean isChanceMatcher = (matcher instanceof ChanceFractionalRewardMatcher || matcher instanceof ChancePercentageRewardMatcher);
                // Break if we've matched a chance award (and we've matched on it), or if this matcher doesn't match.
                if ((chanceMatched && isChanceMatcher) || !matcher.matches(vote, pv)) {
                    continue rewardCheck;
                }
                if (isChanceMatcher) {
                    rewardMatchChance = true;
                }
            }

            best.add(reward);
            if (!reward.isCascade()) break;
            if (rewardMatchChance) chanceMatched = true;
        }

        return best;
    }

    public boolean requirePlayersOnline() {
        return configuration.getBoolean("require-online", false);
    }

    public static String replaceCommandPlaceholders(String text, Vote vote) {
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
            case "mysql":
                String host = configuration.getString("storage.mysql.host", "localhost");
                int port = configuration.getInt("storage.mysql.port", 3306);
                String username = configuration.getString("storage.mysql.username", "root");
                String password = configuration.getString("storage.mysql.password", "");
                String database = configuration.getString("storage.mysql.database", "superbvote");
                String table = configuration.getString("storage.mysql.table", "superbvote");
                boolean readOnly = configuration.getBoolean("storage.mysql.read-only");

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);
                config.setMinimumIdle(2);
                config.setMaximumPoolSize(6);
                HikariPool pool = new HikariPool(config);
                MysqlVoteStorage mysqlVoteStorage = new MysqlVoteStorage(pool, table, readOnly);
                mysqlVoteStorage.initialize();
                return mysqlVoteStorage;
        }

        return null;
    }
}
