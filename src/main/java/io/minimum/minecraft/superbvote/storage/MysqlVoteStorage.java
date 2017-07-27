package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.pool.HikariPool;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@RequiredArgsConstructor
public class MysqlVoteStorage implements VoteStorage {
    public static final int TABLE_VERSION_2 = 2;
    public static final int TABLE_VERSION = TABLE_VERSION_2;

    private final HikariPool dbPool;
    private final String tableName;
    private final boolean readOnly;

    public void initialize() {
        // Load current DB version from disk
        YamlConfiguration dbInfo = YamlConfiguration.loadConfiguration(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
        dbInfo.options().header("DO NOT EDIT - SuperbVote internal use");
        int ver = dbInfo.getInt("db_version", 1);
        boolean isUpdated = false;

        try (Connection connection = dbPool.getConnection()) {
            try (ResultSet t = connection.getMetaData().getTables(null, null, tableName, null)) {
                if (!t.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE TABLE " + tableName + " (uuid VARCHAR(36) PRIMARY KEY NOT NULL, last_name VARCHAR(16), votes INT, last_vote TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                        // This may speed up leaderboards
                        statement.executeUpdate("CREATE INDEX uuid_votes_idx ON " + tableName + " (uuid, votes)");
                    }
                } else {
                    if (ver < TABLE_VERSION) {
                        SuperbVote.getPlugin().getLogger().log(Level.INFO, "Migrating database from version " + ver + " to " + TABLE_VERSION + ", this may take a while...");
                        // We may need to add in the new last_vote column
                        if (ver < TABLE_VERSION_2) {
                            try (Statement statement = connection.createStatement()) {
                                statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN last_vote TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                            }
                            isUpdated = true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to initialize database", e);
        }

        if (isUpdated) {
            dbInfo.set("db_version", TABLE_VERSION);
            try {
                dbInfo.save(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
            } catch (IOException e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to save DB info", e);
            }
        }
    }

    @Override
    public void issueVote(Vote vote) {
        addVote(vote.getUuid(), vote.getName());
    }

    @Override
    public void addVote(UUID player) {
        addVote(player, null);
    }

    public void addVote(UUID player, String knownName) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            if (knownName != null) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?, 1)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1, last_name = ?")) {
                    statement.setString(1, player.toString());
                    statement.setString(2, knownName);
                    statement.setString(3, knownName);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, NULL, 1)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1")) {
                    statement.setString(1, player.toString());
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to add vote for " + player.toString(), e);
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
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to add vote for " + player.toString(), e);
        }
    }

    @Override
    public void setVotes(UUID player, int votes) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE votes = ?")) {
                statement.setString(1, player.toString());
                statement.setInt(2, votes);
                statement.setInt(3, votes);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to set votes for " + player.toString(), e);
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
    public int getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT votes FROM " + tableName + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get votes for " + player.toString(), e);
            return 0;
        }
    }

    @Override
    public List<UUID> getTopVoters(int amount, int page) {
        int offset = page * amount;
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM " + tableName + " ORDER BY votes DESC " +
                    "LIMIT " + amount + " OFFSET " + offset)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<UUID> uuids = new ArrayList<>();
                    while (resultSet.next()) {
                        uuids.add(UUID.fromString(resultSet.getString(1)));
                    }
                    return uuids;
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
    public void save() {
        // No-op
    }
}
