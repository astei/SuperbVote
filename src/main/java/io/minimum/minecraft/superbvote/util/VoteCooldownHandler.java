package io.minimum.minecraft.superbvote.util;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VoteCooldownHandler {
    private final ConcurrentMap<UUID, Map<String, LocalDateTime>> cooldowns = new ConcurrentHashMap<>(32, 0.75f, 2);

    public boolean triggerCooldown(Vote vote) {
        Preconditions.checkNotNull(vote, "vote");

        Map<String, LocalDateTime> lastVoteMap = cooldowns.computeIfAbsent(vote.getUuid(), (ignored) -> new ConcurrentHashMap<>(8, 0.75f, 2));
        LocalDateTime lastTime = lastVoteMap.get(vote.getServiceName());
        LocalDateTime now = LocalDateTime.now();
        if (lastTime == null || lastTime.isBefore(now.minusSeconds(
                SuperbVote.getPlugin().getConfig().getInt("votes.cooldown-per-service", 3600)))) {
            lastVoteMap.put(vote.getServiceName(), now);
            return false;
        }
        return true;
    }
}
