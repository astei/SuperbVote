package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.zaxxer.hikari.pool.HikariPool;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.StreaksConfiguration;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.VoteStreak;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MysqlVoteStorage implements ExtendedVoteStorage {
    private static final int TABLE_VERSION_2 = 2;
    private static final int TABLE_VERSION_3 = 3;
    private static final int TABLE_VERSION_4 = 4;
    private static final int TABLE_VERSION_CURRENT = TABLE_VERSION_4;

    private final HikariPool dbPool;
    private final String tableName, streaksTableName;
    private final boolean readOnly;

    public void initialize() {
        // Load current DB version from disk
        YamlConfiguration dbInfo = YamlConfiguration.loadConfiguration(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
        dbInfo.options().header("DO NOT EDIT - SuperbVote internal use");
        int ver = dbInfo.getInt("db_version", 1);
        boolean isUpdated = false;

        if (!readOnly) {
            try (Connection connection = dbPool.getConnection()) {
                try (ResultSet t = connection.getMetaData().getTables(null, null, tableName, null)) {
                    if (!t.next()) {
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate("CREATE TABLE " + tableName + " (uuid VARCHAR(36) PRIMARY KEY NOT NULL, last_name VARCHAR(16), votes INT NOT NULL, last_vote TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                            // This may speed up leaderboards
                            statement.executeUpdate("CREATE INDEX uuid_votes_idx ON " + tableName + " (uuid, votes)");
                        }
                        isUpdated = true;
                    } else {
                        if (ver < TABLE_VERSION_CURRENT) {
                            SuperbVote.getPlugin().getLogger().log(Level.INFO, "Migrating database from version " + ver + " to " + TABLE_VERSION_CURRENT + ", this may take a while...");
                            // We may need to add in the new last_vote column
                            if (ver < TABLE_VERSION_2) {
                                try (Statement statement = connection.createStatement()) {
                                    statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN last_vote TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                                }
                                isUpdated = true;
                            }
                            if (ver < TABLE_VERSION_3) {
                                try (Statement statement = connection.createStatement()) {
                                    statement.executeUpdate("ALTER TABLE " + tableName + " CHANGE last_vote last_vote TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
                                }
                                isUpdated = true;
                            }
                            if (ver < TABLE_VERSION_4) {
                                try (Statement statement = connection.createStatement()) {
                                    // In case invalid vote counts snuck in, tell MySQL to say it's zero.
                                    statement.executeUpdate("ALTER TABLE " + tableName + " MODIFY votes int(11) NOT NULL DEFAULT 0");
                                }
                            }
                        }
                    }
                }

                if (SuperbVote.getPlugin().getConfiguration().getStreaksConfiguration().isEnabled()) {
                    try (ResultSet t = connection.getMetaData().getTables(null, null, streaksTableName, null)) {
                        if (!t.next()) {
                            try (Statement statement = connection.createStatement()) {
                                statement.executeUpdate("CREATE TABLE " + streaksTableName + " (uuid VARCHAR(36) PRIMARY KEY NOT NULL, streak INT NOT NULL DEFAULT 1, days INT NOT " +
                                        "NULL DEFAULT 1, last_day DATE NOT NULL DEFAULT CURRENT_DATE(), services TEXT NOT NULL DEFAULT '{}', " +
                                        "CHECK(JSON_VALID(services)))");
                                statement.executeUpdate("CREATE INDEX uuid_last_day_idx ON " + streaksTableName + " (uuid, last_day)");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to initialize database", e);
            }

            if (isUpdated) {
                dbInfo.set("db_version", TABLE_VERSION_CURRENT);
                try {
                    dbInfo.save(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
                } catch (IOException e) {
                    SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to save DB info", e);
                }
            }
        }
    }

    @Override
    public void addVote(Vote vote) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(vote, "vote");
        try (Connection connection = dbPool.getConnection()) {
            if (vote.getName() != null) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, last_name, votes, last_vote) VALUES (?, ?, 1, ?)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1, last_name = ?, last_vote = ?")) {
                    statement.setString(1, vote.getUuid().toString());
                    statement.setString(2, vote.getName());
                    statement.setTimestamp(3, new Timestamp(vote.getReceived().getTime()));
                    statement.setString(4, vote.getName());
                    statement.setTimestamp(5, new Timestamp(vote.getReceived().getTime()));
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, last_name, votes, last_vote) VALUES (?, NULL, 1, ?)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1, last_vote = ?")) {
                    statement.setString(1, vote.getUuid().toString());
                    statement.setTimestamp(2, new Timestamp(vote.getReceived().getTime()));
                    statement.setTimestamp(3, new Timestamp(vote.getReceived().getTime()));
                    statement.executeUpdate();
                }
            }

            if (SuperbVote.getPlugin().getConfiguration().getStreaksConfiguration().isEnabled()) {
                VoteStreak currentStreak = getVoteStreak(vote.getUuid(), true);
                if (currentStreak.getCount() == 0 && currentStreak.getDays() == 0) {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + streaksTableName + " (uuid, services) VALUES (?, JSON_SET('{}', ?, " +
                            "UNIX_TIMESTAMP())) ON DUPLICATE KEY UPDATE streak = 1, days = 1, services = JSON_SET('{}', ?, UNIX_TIMESTAMP()), last_day = CURRENT_DATE()")) {
                        statement.setString(1, vote.getUuid().toString());
                        String serviceKey = "$." + vote.getServiceName();
                        statement.setString(2, serviceKey);
                        statement.setString(3, serviceKey);
                        statement.executeUpdate();
                    }
                }
                else {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + streaksTableName + " (uuid, services) VALUES (?, JSON_SET('{}', ?, " +
                            "UNIX_TIMESTAMP())) ON DUPLICATE KEY UPDATE streak = streak + 1, days = days + LEAST(1, DATEDIFF(CURRENT_DATE(), last_day))," +
                            " services = JSON_SET(services, ?, UNIX_TIMESTAMP()), last_day = CURRENT_DATE()")) {
                        statement.setString(1, vote.getUuid().toString());
                        String serviceKey = "$." + vote.getServiceName();
                        statement.setString(2, serviceKey);
                        statement.setString(3, serviceKey);
                        statement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add vote for " + vote.getUuid().toString(), e);
        }
    }

    public void updateName(Player player) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + tableName + " SET last_name = ? WHERE uuid = ?")) {
                statement.setString(1, player.getName());
                statement.setString(2, player.getUniqueId().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update name for " + player.toString(), e);
        }
    }

    @Override
    public void setVotes(UUID player, int votes, long ts) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, votes, last_vote) VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE votes = ?, last_vote = ?")) {
                statement.setString(1, player.toString());
                statement.setInt(2, votes);
                statement.setTimestamp(3, new Timestamp(ts));
                statement.setInt(4, votes);
                statement.setTimestamp(5, new Timestamp(ts));
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to set votes for " + player.toString(), e);
        }
    }

    @Override
    public void clearVotes() {
        if (readOnly)
            return;

        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE " + tableName)) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to clear votes", e);
        }
    }

    @Override
    public PlayerVotes getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT last_name, votes FROM " + tableName + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new PlayerVotes(player, resultSet.getString(1), resultSet.getInt(2), PlayerVotes.Type.CURRENT);
                    } else {
                        return new PlayerVotes(player, null, 0, PlayerVotes.Type.CURRENT);
                    }
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get votes for " + player.toString(), e);
            return new PlayerVotes(player, null, 0, PlayerVotes.Type.CURRENT);
        }
    }

    @Override
    public List<PlayerVotes> getTopVoters(int amount, int page) {
        int offset = page * amount;
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, votes FROM " + tableName + " WHERE votes > 0 ORDER BY votes DESC " +
                    "LIMIT " + amount + " OFFSET " + offset)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<PlayerVotes> records = new ArrayList<>();
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString(1));
                        String name = resultSet.getString(2);
                        records.add(new PlayerVotes(uuid, name, resultSet.getInt(3), PlayerVotes.Type.CURRENT));
                    }
                    return records;
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes", e);
            return Collections.emptyList();
        }
    }


    @Override
    public int getPagesAvailable(int amount) {
        try (Connection connection = dbPool.getConnection()) {
            // Ugly SQL, but who cares
            try (PreparedStatement statement = connection.prepareStatement("SELECT CEIL(COUNT(uuid) / " + amount + ") FROM " + tableName)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes page count", e);
            return 0;
        }
    }

    @Override
    public boolean hasVotedToday(UUID player) {
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + tableName + " WHERE uuid = ? AND DATE(last_vote) = CURRENT_DATE()")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes page count", e);
            return false;
        }
    }

    @Override
    public List<PlayerVotes> getAllPlayersWithNoVotesToday(List<UUID> onlinePlayers) {
        if (onlinePlayers.isEmpty()) {
            return ImmutableList.of();
        }
        List<PlayerVotes> votes = new ArrayList<>();
        try (Connection connection = dbPool.getConnection()) {
            String valueStatement = Joiner.on(", ").join(Collections.nCopies(onlinePlayers.size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, votes, (DATE(last_vote) = CURRENT_DATE()) AS has_voted_today FROM " + tableName + " WHERE uuid IN (" + valueStatement + ")")) {
                for (int i = 0; i < onlinePlayers.size(); i++) {
                    statement.setString(i + 1, onlinePlayers.get(i).toString());
                }
                List<UUID> found = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString(1));
                        found.add(uuid);
                        if (resultSet.getBoolean(4)) {
                            continue; // already voted today
                        }
                        PlayerVotes pv = new PlayerVotes(UUID.fromString(resultSet.getString(1)),
                                resultSet.getString(2),
                                resultSet.getInt(3),
                                PlayerVotes.Type.CURRENT);
                        votes.add(pv);
                    }
                }

                // We may have players without a voting record. Add these missing players.
                List<UUID> missing = new ArrayList<>(onlinePlayers);
                missing.removeAll(found);
                for (UUID uuid : missing) {
                    votes.add(new PlayerVotes(uuid, null, 0, PlayerVotes.Type.CURRENT));
                }
            }
            return votes;
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to batch-get votes", e);
            return ImmutableList.of();
        }
    }

    @Override
    public List<Map.Entry<PlayerVotes, VoteStreak>> getAllPlayersAndStreaksWithNoVotesToday(List<UUID> onlinePlayers) {
        Map<UUID, PlayerVotes> noVotes = getAllPlayersWithNoVotesToday(onlinePlayers).stream()
                .collect(Collectors.toMap(PlayerVotes::getUuid, p -> p));
        if (noVotes.isEmpty()) {
            return ImmutableList.of();
        }

        List<Map.Entry<PlayerVotes, VoteStreak>> result = new ArrayList<>();
        try (Connection connection = dbPool.getConnection()) {
            String valueStatement = Joiner.on(", ").join(Collections.nCopies(noVotes.size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, streak, days FROM " + streaksTableName + " WHERE uuid IN (" + valueStatement + ")")) {
                List<PlayerVotes> noVotesList = new ArrayList<>(noVotes.values());
                for (int i = 0; i < noVotesList.size(); i++) {
                    statement.setString(i + 1, noVotesList.get(i).toString());
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString(1));
                        result.add(Maps.immutableEntry(noVotes.get(uuid), new VoteStreak(uuid,
                                resultSet.getInt(2),
                                resultSet.getInt(3),
                                // does not parse services timestamps here since it's not queried afterward
                                new HashMap<>())));
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to batch-get votes", e);
            return ImmutableList.of();
        }
    }

    private final Type servicesMapType = new TypeToken<Map<String, Long>>(){}.getType();
    private final Gson gson = new Gson();

    @Override
    public VoteStreak getVoteStreak(UUID player, boolean required) {
        Preconditions.checkNotNull(player, "player");
        StreaksConfiguration streaksConfiguration = SuperbVote.getPlugin().getConfiguration().getStreaksConfiguration();
        if (!streaksConfiguration.isEnabled() || (!required && !streaksConfiguration.isPlaceholdersEnabled())) {
            return null;
        }

        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT streak, days, DATEDIFF(CURRENT_DATE(), last_day), services, UNIX_TIMESTAMP() FROM " + streaksTableName
                                 + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int daysDifference = resultSet.getInt(3);
                        // e.g: last vote on 10/03 11 PM,
                        //        on 10/04 -> 1 day difference, player will be able to vote at 11 PM, leaving him 1 hour to remember about it
                        //        on 10/05 -> 2 days difference, hasn't voted but can vote until 11:59 PM, total of 25 hours to think about voting
                        //        on 10/06 -> 3 days difference, hasn't voted, break the streak
                        // this can be improved with a player-based time, leaving him exactly the required interval of ]24;48[h to vote
                        if (daysDifference > 2) {
                            // reset vote streak
                            try (PreparedStatement resetStatement = connection.prepareStatement("UPDATE " + streaksTableName + " SET streak = 0, days = 0, services = '{}'" +
                                    " WHERE uuid = ?")) {
                                resetStatement.setString(1, player.toString());
                                resetStatement.executeUpdate();
                            }
                            return new VoteStreak(player, 0, 0, Maps.newHashMap());
                        }

                        // services : map a service name to time since last vote in seconds
                        Map<String, Long> services = gson.fromJson(resultSet.getString(4), servicesMapType);
                        long unixTimestamp = resultSet.getLong(5);
                        return new VoteStreak(player, resultSet.getInt(1), resultSet.getInt(2), services.entrySet().stream()
                                .map(entry -> Maps.immutableEntry(entry.getKey(), unixTimestamp - entry.getValue()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                    } else {
                        return new VoteStreak(player, 0, 0, Maps.newHashMap());
                    }
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get or reset vote streak for " + player.toString(), e);
            return new VoteStreak(player, 0, 0, Maps.newHashMap());
        }
    }

    @Override
    public void save() {
        // No-op
    }

    @Override
    public void close() {
        try {
            dbPool.shutdown();
        } catch (InterruptedException e) {
            // Not much we can do about that...
        }
    }
}
