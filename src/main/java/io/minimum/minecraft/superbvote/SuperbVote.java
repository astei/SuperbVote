package io.minimum.minecraft.superbvote;

import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvote.scoreboard.ScoreboardHandler;
import io.minimum.minecraft.superbvote.storage.QueuedVotesStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.uuid.UuidCache;
import io.minimum.minecraft.superbvote.votes.SuperbVoteHandler;
import io.minimum.minecraft.superbvote.votes.SuperbVoteListener;
import io.minimum.minecraft.superbvote.votes.VoteReminder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class SuperbVote extends JavaPlugin {
    @Getter
    private static SuperbVote plugin;
    @Getter
    private SuperbVoteConfiguration configuration;
    @Getter
    private VoteStorage voteStorage;
    @Getter
    private UuidCache uuidCache = new UuidCache();
    @Getter
    private QueuedVotesStorage queuedVotes;
    @Getter
    private ScoreboardHandler scoreboardHandler;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        configuration = new SuperbVoteConfiguration(getConfig());

        try {
            voteStorage = configuration.initializeVoteStorage();
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst initializing vote storage", e);
        }

        try {
            queuedVotes = new QueuedVotesStorage(new File(getDataFolder(), "queued_votes.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst initializing queued vote storage", e);
        }

        scoreboardHandler = new ScoreboardHandler();

        getCommand("superbvote").setExecutor(new SuperbVoteCommand());
        getServer().getPluginManager().registerEvents(new SuperbVoteListener(), this);
        getServer().getPluginManager().registerEvents(new SuperbVoteHandler(), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, voteStorage::save, 20, 20 * 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this, queuedVotes::save, 20, 20 * 30);
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), SuperbVote.getPlugin().getScoreboardHandler()::doPopulate);

        int r = getConfig().getInt("vote-reminder.repeat");
        String text = SuperbVote.getPlugin().getConfig().getString("vote-reminder.message");
        if (text != null && !text.isEmpty()) {
            if (r > 0) {
                getServer().getScheduler().runTaskTimerAsynchronously(this, new VoteReminder(), 20 * r, 20 * r);
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }
    }

    @Override
    public void onDisable() {
        voteStorage.save();
        queuedVotes.save();
    }

    public void reloadPlugin() {
        reloadConfig();
        configuration = new SuperbVoteConfiguration(getConfig());
        scoreboardHandler.reload();
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), SuperbVote.getPlugin().getScoreboardHandler()::doPopulate);
    }
}
