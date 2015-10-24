package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueuedVotesStorage {
    private final Map<UUID, List<Vote>> voteCounts = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Gson gson = new Gson();
    private final File saveTo;

    public QueuedVotesStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new BufferedReader(new FileReader(file))) {
            Map<UUID, List<Vote>> votes = gson.fromJson(reader, new TypeToken<Map<UUID, List<Vote>>>() {
            }.getType());
            if (votes != null) {
                for (Map.Entry<UUID, List<Vote>> entry : votes.entrySet()) {
                    voteCounts.put(entry.getKey(), new CopyOnWriteArrayList<>(entry.getValue()));
                }
            }
        }
    }

    public void addVote(Vote vote) {
        Preconditions.checkNotNull(vote, "votes");
        List<Vote> votes = voteCounts.computeIfAbsent(vote.getUuid(), (ignored) -> new CopyOnWriteArrayList<>());
        votes.add(vote);
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
        try (Writer writer = new BufferedWriter(new FileWriter(saveTo))) {
            gson.toJson(voteCounts, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo.getAbsolutePath(), e);
        }
    }
}
