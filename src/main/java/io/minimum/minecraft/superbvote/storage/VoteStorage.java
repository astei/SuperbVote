package io.minimum.minecraft.superbvote.storage;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.util.List;
import java.util.UUID;

public interface VoteStorage {
    void addVote(Vote vote);

    default void setVotes(UUID player, int votes) {
        setVotes(player, votes, System.currentTimeMillis());
    }

    void setVotes(UUID player, int votes, long ts);

    void clearVotes();

    PlayerVotes getVotes(UUID player);

    List<PlayerVotes> getTopVoters(int amount, int page);

    int getPagesAvailable(int amount);

    boolean hasVotedToday(UUID player);

    List<PlayerVotes> getAllPlayersWithNoVotesToday(List<UUID> onlinePlayers);

    void save();

    void close();
}
