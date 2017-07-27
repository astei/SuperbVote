package io.minimum.minecraft.superbvote.storage;

import io.minimum.minecraft.superbvote.votes.Vote;

import java.util.List;
import java.util.UUID;

public interface VoteStorage {
    void issueVote(Vote vote);

    void addVote(UUID player);

    void setVotes(UUID player, int votes);

    void clearVotes();

    int getVotes(UUID player);

    List<UUID> getTopVoters(int amount, int page);

    int getPagesAvailable(int amount);

    boolean hasVotedToday(UUID player);

    void save();
}
