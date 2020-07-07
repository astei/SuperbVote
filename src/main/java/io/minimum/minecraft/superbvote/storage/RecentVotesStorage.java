package io.minimum.minecraft.superbvote.storage;

import io.minimum.minecraft.superbvote.SuperbVote;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RecentVotesStorage {

    private final Map<UUID, Instant> lastVotes = new ConcurrentHashMap<>(32, 0.75f, 2);

    public boolean canBroadcast(UUID uuid) {
        if (!SuperbVote.getPlugin().getConfig().getBoolean("broadcast.antispam.enabled")) return true;
        Instant start = lastVotes.getOrDefault(uuid, Instant.MIN);
        int antispamTime = SuperbVote.getPlugin().getConfig().getInt("broadcast.antispam.time", 120);
        return Duration.between(start, Instant.now()).getSeconds() >= antispamTime;
    }

    public void updateLastVote(UUID uuid) {
        lastVotes.put(uuid, Instant.now());
    }
}
