package io.minimum.minecraft.superbvote.storage;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.VoteStreak;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExtendedVoteStorage extends VoteStorage {
    VoteStreak getVoteStreak(UUID player, boolean required);

    List<Map.Entry<PlayerVotes, VoteStreak>> getAllPlayersAndStreaksWithNoVotesToday(List<UUID> onlinePlayers);
}
