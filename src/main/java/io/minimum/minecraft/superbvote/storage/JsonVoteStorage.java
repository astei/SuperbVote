package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class JsonVoteStorage implements VoteStorage {
    private static final int VERSION = 2;

    private final Map<UUID, PlayerRecord> voteCounts = new HashMap<>();
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Gson gson = new Gson();
    private final Path saveTo;

    public JsonVoteStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file.toPath();

        if (!file.exists()) file.createNewFile();

        boolean needMigrate = false;
        try (Reader reader = Files.newBufferedReader(saveTo)) {
            VotingFile vf = gson.fromJson(reader, VotingFile.class);
            if (vf == null || vf.records == null || vf.records.isEmpty()) {
                needMigrate = true;
            } else {
                voteCounts.putAll(vf.records);
            }
        }

        if (needMigrate) {
            Map<UUID, Integer> votesToMigrate = null;
            try (Reader reader = Files.newBufferedReader(saveTo)) {
                votesToMigrate = gson.fromJson(reader, new TypeToken<Map<UUID, Integer>>() {
                }.getType());
            }
            if (votesToMigrate != null) {
                VotingFile vf = migrateOldVersion(votesToMigrate);
                voteCounts.putAll(vf.records);
            }
        }
    }

    private VotingFile migrateOldVersion(Map<UUID, Integer> votes) throws IOException {
        // We'll need to convert old records then
        SuperbVote.getPlugin().getLogger().info("Migrating records to new file format, this may take a while...");
        Map<UUID, PlayerRecord> records = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
            records.put(entry.getKey(), new PlayerRecord(entry.getValue()));
        }
        VotingFile vf = new VotingFile(VERSION, records);

        // Save the new file
        Path tempPath = Files.createTempFile("superbvote-", ".json");
        try (Writer writer = Files.newBufferedWriter(tempPath, StandardOpenOption.WRITE)) {
            gson.toJson(vf, writer);
        }

        // Move the old files out of the way and move in the new one
        Files.copy(saveTo, saveTo.getParent().resolve(saveTo.getFileName().toString() + "-migrated"));
        Files.copy(tempPath, saveTo, StandardCopyOption.REPLACE_EXISTING);

        // Done!
        return vf;
    }

    @Override
    public void issueVote(Vote vote) {
        addVote(vote.getUuid());
    }

    @Override
    public void addVote(UUID player) {
        Preconditions.checkNotNull(player, "player");
        rwl.writeLock().lock();
        try {
            PlayerRecord rec = voteCounts.putIfAbsent(player, new PlayerRecord(1));
            if (rec != null) {
                rec.votes++;
                rec.lastVoted = System.currentTimeMillis();
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void setVotes(UUID player, int votes) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkArgument(votes >= 0, "votes out of bound");
        rwl.writeLock().lock();
        try {
            if (votes == 0) {
                voteCounts.remove(player);
            } else {
                PlayerRecord rec = voteCounts.putIfAbsent(player, new PlayerRecord(votes));
                if (rec != null) {
                    rec.votes = votes;
                }
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void clearVotes() {
        rwl.writeLock().lock();
        try {
            voteCounts.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public int getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        rwl.readLock().lock();
        try {
            PlayerRecord pr = voteCounts.get(player);
            return pr == null ? 0 : pr.votes;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public List<UUID> getTopVoters(int amount, int page) {
        int skip = page * amount;
        rwl.readLock().lock();
        try {
            return voteCounts.entrySet().stream()
                    .sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
                    .skip(skip)
                    .limit(amount)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public int getPagesAvailable(int amount) {
        rwl.readLock().lock();
        try {
            if (voteCounts.isEmpty()) return 0;
            return Math.max(1, (int) Math.ceil(voteCounts.size() / amount));
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public boolean hasVotedToday(UUID player) {
        Preconditions.checkNotNull(player, "player");
        rwl.readLock().lock();
        try {
            PlayerRecord pr = voteCounts.get(player);
            if (pr != null) {
                Calendar then = Calendar.getInstance();
                then.setTimeInMillis(pr.lastVoted);
                Calendar today = Calendar.getInstance();
                return then.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        then.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        then.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
            }
            return false;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void save() {
        Map<UUID, PlayerRecord> prs;
        rwl.readLock().lock();
        try {
            prs = new HashMap<>();
            for (Map.Entry<UUID, PlayerRecord> entry : voteCounts.entrySet()) {
                prs.put(entry.getKey(), new PlayerRecord(entry.getValue().votes, entry.getValue().lastVoted));
            }
        } finally {
            rwl.readLock().unlock();
        }
        try (Writer writer = Files.newBufferedWriter(saveTo, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(new VotingFile(VERSION, prs), writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo, e);
        }
    }

    private static class VotingFile {
        private final int version;
        private final Map<UUID, PlayerRecord> records;

        private VotingFile(int version, Map<UUID, PlayerRecord> records) {
            this.version = version;
            this.records = records;
        }
    }

    private static class PlayerRecord implements Comparable<PlayerRecord> {
        private int votes;
        private long lastVoted;

        private PlayerRecord(int votes) {
            this(votes, System.currentTimeMillis());
        }

        private PlayerRecord(int votes, long lastVoted) {
            this.votes = votes;
            this.lastVoted = lastVoted;
        }

        @Override
        public int compareTo(PlayerRecord o) {
            return Integer.compare(votes, o.votes);
        }
    }
}
