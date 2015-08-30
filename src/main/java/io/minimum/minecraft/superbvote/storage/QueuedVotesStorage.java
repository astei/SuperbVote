package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QueuedVotesStorage {
    private final Map<UUID, List<Vote>> voteCounts = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Gson gson = new Gson();
    private final File saveTo;

    public QueuedVotesStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new FileReader(file)) {
            Map<UUID, List<Vote>> votes = gson.fromJson(reader, new TypeToken<Map<UUID, List<Vote>>>() {
            }.getType());
            if (votes != null) voteCounts.putAll(votes);
        }
    }

    public void addVote(Vote vote) {
        Preconditions.checkNotNull(vote, "votes");
        voteCounts.compute(vote.getUuid(), (key, votes) -> {
            if (votes == null) {
                return Lists.newArrayList(vote);
            } else {
                votes.add(vote);
                return votes;
            }
        });
    }

    public void clearVotes() {
        voteCounts.clear();
    }

    public List<Vote> getAndRemoveVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        List<Vote> votes = voteCounts.remove(player);
        return votes != null ? votes : ImmutableList.of();
    }

    public void save() {
        try (Writer writer = new FileWriter(saveTo)) {
            gson.toJson(voteCounts, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo.getAbsolutePath(), e);
        }
    }
}
