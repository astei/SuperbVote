package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class JsonVoteStorage implements VoteStorage {
    private final ConcurrentMap<UUID, Integer> voteCounts = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Gson gson = new Gson();
    private final File saveTo;

    public JsonVoteStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new FileReader(file)) {
            Map<UUID, Integer> votes = gson.fromJson(reader, new TypeToken<Map<UUID, Integer>>() {
            }.getType());
            if (votes != null) voteCounts.putAll(votes);
        }
    }

    @Override
    public void addVote(UUID player) {
        Preconditions.checkNotNull(player, "player");
        voteCounts.compute(player, (key, vc) -> vc == null ? 1 : vc + 1);
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
        return voteCounts.size() / amount;
    }

    @Override
    public void save() {
        try (Writer writer = new FileWriter(saveTo)) {
            gson.toJson(voteCounts, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo.getAbsolutePath(), e);
        }
    }
}
