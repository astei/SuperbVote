package io.minimum.minecraft.superbvote;

import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvote.uuid.OfflineModeUuidCache;
import io.minimum.minecraft.superbvote.uuid.OnlineModeUuidCache;
import io.minimum.minecraft.superbvote.votes.SuperbVoteHandler;
import io.minimum.minecraft.superbvote.votes.SuperbVoteListener;
import io.minimum.minecraft.superbvote.storage.QueuedVotesStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.uuid.UuidCache;
import lombok.Getter;
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
    private UuidCache uuidCache;
    @Getter
    private QueuedVotesStorage queuedVotes;

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

        uuidCache = configuration.initializeUuidCache();

        getCommand("superbvote").setExecutor(new SuperbVoteCommand());
        getServer().getPluginManager().registerEvents(new SuperbVoteListener(), this);
        getServer().getPluginManager().registerEvents(new SuperbVoteHandler(), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, voteStorage::save, 20, 20 * 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this, queuedVotes::save, 20, 20 * 30);

        if (uuidCache instanceof OfflineModeUuidCache) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, ((OfflineModeUuidCache) uuidCache)::save, 20, 20 * 30);
        }
    }

    @Override
    public void onDisable() {
        voteStorage.save();
        queuedVotes.save();
        if (uuidCache instanceof OfflineModeUuidCache) {
            ((OfflineModeUuidCache) uuidCache).save();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        configuration = new SuperbVoteConfiguration(getConfig());
    }
}
