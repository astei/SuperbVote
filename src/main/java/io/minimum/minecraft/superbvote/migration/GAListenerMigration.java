package io.minimum.minecraft.superbvote.migration;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class GAListenerMigration implements Migration {
    @Override
    public void execute() {
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

        try (PreparedStatement statement = connection.prepareStatement("SELECT UUID, votes FROM " + prefix + "GALTotals");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                int votes = rs.getInt(2);

                SuperbVote.getPlugin().getVoteStorage().setVotes(uuid, votes);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to migrate database", e);
        }
    }
}
