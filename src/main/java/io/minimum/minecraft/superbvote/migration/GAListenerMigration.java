package io.minimum.minecraft.superbvote.migration;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class GAListenerMigration implements Migration {
    @Override
    public String getName() {
        return "GAListener";
    }

    @Override
    public void execute(ProgressListener listener) {
        File galRoot = new File(SuperbVote.getPlugin().getDataFolder(), ".." + File.separator + "GAListener");
        File galConfig = new File(galRoot, "config.yml");

        if (!galConfig.exists() || !galConfig.isFile()) {
            throw new RuntimeException("GAListener configuration does not exist.");
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(galConfig);

        Connection connection;
        String prefix = configuration.getString("settings.dbPrefix");

        try {
            switch (configuration.getString("settings.dbMode")) {
                case "sqlite":
                    connection = DriverManager.getConnection("jdbc:sqlite:" +
                            new File(galRoot, configuration.getString("settings.dbFile")).getAbsolutePath());
                    break;
                case "mysql":
                    String host = configuration.getString("settings.dbHost");
                    int port = configuration.getInt("settings.dbPort");
                    String user = configuration.getString("settings.dbUser");
                    String pass = configuration.getString("settings.dbPass");
                    String name = configuration.getString("settings.dbName");
                    connection = DriverManager.getConnection(
                            "jdbc:mysql://" + host + ":" + port + "/" + name, user, pass
                    );
                    break;
                default:
                    throw new RuntimeException("'" + configuration.getString("settings.dbMode") + "' is not a supported database");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to open connection to GAListener DB", e);
        }

        try {
            // Find out how many records we have to convert.
            int records = 0;
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(UUID) FROM " + prefix + "GALTotals");
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    records = rs.getInt(1);
                }
            }

            // Actually migrate the records.
            listener.onStart(records);
            int divisor = ProgressUtil.findBestDivisor(records);
            int currentIdx = 0;
            try (PreparedStatement statement = connection.prepareStatement("SELECT UUID, votes, lastvoted FROM " + prefix + "GALTotals");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString(1));
                    int votes = rs.getInt(2);
                    long lastVoted = rs.getLong(3);
                    SuperbVote.getPlugin().getVoteStorage().setVotes(uuid, votes, lastVoted);

                    currentIdx++;
                    if (currentIdx % divisor == 0) {
                        listener.onRecordBatch(currentIdx, records);
                    }
                }
            }

            listener.onFinish(records);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to migrate database", e);
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Ignore. How can we possibly clean this up?!?
            }
        }
    }
}
