package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.*;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class JsonVoteStorage implements VoteStorage {
    private final ConcurrentMap<UUID, Integer> voteCounts = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final ConcurrentMap<UUID, Map<String, LocalTime>> cooldowns = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Gson gson = new Gson();
    private final File saveTo;

    public JsonVoteStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new BufferedReader(new FileReader(file))) {
            Map<UUID, Integer> votes = gson.fromJson(reader, new TypeToken<Map<UUID, Integer>>() {
            }.getType());
            if (votes != null) voteCounts.putAll(votes);
        }
    }

    @Override
    public void issueVote(Vote vote) {
        addVote(vote.getUuid());
    }

    @Override
    public void addVote(UUID player) {
        Preconditions.checkNotNull(player, "player");
        voteCounts.compute(player, (key, vc) -> vc == null ? 1 : vc + 1);
    }

    @Override
    public void setVotes(UUID player, int votes) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkArgument(votes >= 0, "votes out of bound");
        if (votes == 0) {
            voteCounts.remove(player);
        } else {
            voteCounts.put(player, votes);
        }
    }

    @Override
    public void clearVotes() {
        voteCounts.clear();
    }

    @Override
    public int getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        return voteCounts.getOrDefault(player, 0);
    }

    @Override
    public List<UUID> getTopVoters(int amount, int page) {
        int skip = page * amount;
        return voteCounts.entrySet().stream()
                .skip(skip)
                .limit(amount)
                .sorted((entry, entry2) -> entry.getValue().compareTo(entry2.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public int getPagesAvailable(int amount) {
        return (int) Math.ceil(voteCounts.size() / amount);
    }

    @Override
    public void save() {
        try (Writer writer = new BufferedWriter(new FileWriter(saveTo))) {
            gson.toJson(voteCounts, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo.getAbsolutePath(), e);
        }
    }
}
