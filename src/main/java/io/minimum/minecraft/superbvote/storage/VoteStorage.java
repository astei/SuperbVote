package io.minimum.minecraft.superbvote.storage;

import java.util.List;
import java.util.UUID;

public interface VoteStorage {
    void addVote(UUID player);
    void clearVotes();
    int getVotes(UUID player);
    List<UUID> getTopVoters(int amount, int page);
    int getPagesAvailable(int amount);
    void save();
}
