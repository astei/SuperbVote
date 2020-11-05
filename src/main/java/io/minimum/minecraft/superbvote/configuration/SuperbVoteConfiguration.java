package io.minimum.minecraft.superbvote.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.CommonCommand;
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
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatcher;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.RewardMatchers;
import io.minimum.minecraft.superbvote.votes.rewards.matchers.StaticRewardMatcher;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class SuperbVoteConfiguration {
    @Getter(AccessLevel.NONE)
    private final ConfigurationSection configuration;

    private final List<VoteReward> rewards = new ArrayList<>();
    private final VoteMessage reminderMessage;
    private final CommonCommand voteCommand, voteStreakCommand;

    private final TextLeaderboardConfiguration textLeaderboardConfiguration;
    private final TopPlayerSignsConfiguration topPlayerSignsConfiguration;

    private final StreaksConfiguration streaksConfiguration;

    private boolean configurationError = false;

    private static final List<String> SUPPORTED_STORAGE = ImmutableList.of("json", "mysql");

    public SuperbVoteConfiguration(ConfigurationSection section) {
        this.configuration = section;

        try {
            section.getList("rewards").stream()
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
                    .forEach(rewards::add);
        } catch (Exception e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to load your reward configuration.", e);
            SuperbVote.getPlugin().getLogger().severe("SuperbVote will be disabled until your configuration is fixed.");
            configurationError = true;
        }

        if (rewards.isEmpty() && !configurationError) {
            SuperbVote.getPlugin().getLogger().severe("Your configuration does not specify any rewards.");
            configurationError = true;
        }

        long defaultRewardCount = rewards.stream()
                .filter(r -> r.getRewardMatchers().contains(StaticRewardMatcher.ALWAYS_MATCH) || r.getRewardMatchers().isEmpty())
                .count();
        if (defaultRewardCount == 0 && !configurationError) {
            SuperbVote.getPlugin().getLogger().severe("No default reward was defined. To set a default reward, set default: true in one of your reward if blocks.");
            configurationError = true;
        } else if (defaultRewardCount > 1 && !configurationError) {
            SuperbVote.getPlugin().getLogger().severe("Multiple default rewards are defined. Hint: You may want to check the spelling in your 'if' blocks. (Further information has been logged.)");
            configurationError = true;
        }

        boolean rewardsNotProperlyConfigured = rewards.stream()
                .anyMatch(r -> r.getRewardMatchers().contains(StaticRewardMatcher.ERROR));
        if (rewardsNotProperlyConfigured) {
            SuperbVote.getPlugin().getLogger().severe("Some of your rewards are not correctly set up. This could cause issues, so SuperbVote will not work until you have fixed your configuration. (Further information has been logged.)");
            configurationError = true;
        }

        reminderMessage = VoteMessages.from(configuration, "vote-reminder.message");

        if (configuration.getBoolean("vote-command.enabled")) {
            boolean useJson = configuration.getBoolean("vote-command.use-json-text");
            VoteMessage voteMessage = VoteMessages.from(configuration, "vote-command.text", false, useJson);
            voteCommand = new CommonCommand(voteMessage, false);
        } else {
            voteCommand = null;
        }

        streaksConfiguration = initializeStreaksConfiguration();
        if (streaksConfiguration.isEnabled() && configuration.getBoolean("streaks.command.enabled")) {
            boolean useJson = configuration.getBoolean("streaks.command.use-json-text");
            VoteMessage voteStreakMessage = VoteMessages.from(configuration, "streaks.command.text", false, useJson);
            voteStreakCommand = new CommonCommand(voteStreakMessage, true);
        } else {
            voteStreakCommand = null;
        }

        textLeaderboardConfiguration = new TextLeaderboardConfiguration(
                configuration.getInt("leaderboard.text.per-page", 10),
                OfflineVoteMessages.from(configuration.getConfigurationSection("leaderboard.text"), "header"),
                OfflineVoteMessages.from(configuration.getConfigurationSection("leaderboard.text"), "entry"),
                OfflineVoteMessages.from(configuration.getConfigurationSection("leaderboard.text"), "page")
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
        for (VoteReward reward : rewards) {
            if (reward.getRewardMatchers().stream().allMatch(matcher -> matcher.matches(vote, pv))) {
                best.add(reward);

                if (!reward.isCascade()) {
                    break;
                }
            }
        }

        return best;
    }

    public boolean requirePlayersOnline() {
        return configuration.getBoolean("require-online", false);
    }

    public boolean shouldQueueVotes() {
        return configuration.getBoolean("queue-votes", true);
    }

    public static String replaceCommandPlaceholders(String text, Vote vote) {
        return text.replaceAll("%player%", vote.getName())
                .replaceAll("%service%", vote.getServiceName())
                .replaceAll("%player_uuid%", vote.getUuid().toString());
    }

    private StreaksConfiguration initializeStreaksConfiguration() {
        ConfigurationSection section = configuration.getConfigurationSection("streaks");
        if (section == null) {
            return new StreaksConfiguration(false, false, false);
        }

        boolean enabled = section.getBoolean("enabled");
        boolean placeholdersEnabled = section.getBoolean("enable-placeholders");
        boolean sharedCooldownPerService = section.getBoolean("shared-cooldown-per-service");
        return new StreaksConfiguration(enabled, enabled && placeholdersEnabled, enabled && sharedCooldownPerService);
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
                String streaksTableName = configuration.getString("storage.mysql.streaks-table");
                boolean readOnly = configuration.getBoolean("storage.mysql.read-only");

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);
                config.setMinimumIdle(2);
                config.setMaximumPoolSize(6);
                HikariPool pool = new HikariPool(config);
                MysqlVoteStorage mysqlVoteStorage = new MysqlVoteStorage(pool, table, streaksTableName, readOnly);
                mysqlVoteStorage.initialize();
                return mysqlVoteStorage;
        }

        return null;
    }
}
