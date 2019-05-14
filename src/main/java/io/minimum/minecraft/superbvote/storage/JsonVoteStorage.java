package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
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
            if (vf == null || vf.records == null) {
                needMigrate = true;
            } else {
                voteCounts.putAll(vf.records);
            }
        } catch (JsonSyntaxException e) {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String movedName = String.format("%s-broken-%s", saveTo.getFileName(), date);
            SuperbVote.getPlugin().getLogger().severe("Your vote storage file is corrupted. Starting fresh by moving it to " + movedName + ".");
            SuperbVote.getPlugin().getLogger().severe("As a result, your votes have been reset. Sorry :(");
            Files.move(saveTo, saveTo.getParent().resolve(movedName));
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
        Files.move(saveTo, saveTo.getParent().resolve(saveTo.getFileName().toString() + "-migrated"));
        Files.move(tempPath, saveTo, StandardCopyOption.REPLACE_EXISTING);

        // Done!
        return vf;
    }

    @Override
    public void addVote(Vote vote) {
        Preconditions.checkNotNull(vote, "vote");
        rwl.writeLock().lock();
        try {
            PlayerRecord rec = voteCounts.putIfAbsent(vote.getUuid(), new PlayerRecord(vote.getName(), 1, vote.getReceived().getTime()));
            if (rec != null) {
                rec.lastKnownUsername = vote.getName();
                rec.votes++;
                rec.lastVoted = vote.getReceived().getTime();
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void setVotes(UUID player, int votes, long ts) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkArgument(votes >= 0, "votes out of bound");
        rwl.writeLock().lock();
        try {
            if (votes == 0) {
                voteCounts.remove(player);
            } else {
                PlayerRecord rec = this.voteCounts.get(player);
                if (rec != null) {
                    rec.votes = votes;
                    rec.lastVoted = ts;
                } else {
                    this.voteCounts.put(player, new PlayerRecord(votes, ts));
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
    public PlayerVotes getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        rwl.readLock().lock();
        try {
            PlayerRecord pr = voteCounts.get(player);
            return new PlayerVotes(player, pr != null ? pr.lastKnownUsername : null, pr == null ? 0 : pr.votes,
                    PlayerVotes.Type.CURRENT);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public List<PlayerVotes> getTopVoters(int amount, int page) {
        int skip = page * amount;
        rwl.readLock().lock();
        try {
            return voteCounts.entrySet().stream()
                    .filter(e -> e.getValue().votes > 0)
                    .sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
                    .skip(skip)
                    .limit(amount)
                    .map(e -> new PlayerVotes(e.getKey(), e.getValue().lastKnownUsername, e.getValue().votes, PlayerVotes.Type.CURRENT))
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
            return (int) Math.ceil(voteCounts.size() / (double) amount);
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
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(pr.lastVoted), ZoneId.systemDefault())
                        .toLocalDate()
                        .equals(LocalDate.now());
            }
            return false;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public List<PlayerVotes> getAllPlayersWithNoVotesToday(List<UUID> onlinePlayers) {
        // generic implementation
        List<PlayerVotes> uuids = new ArrayList<>();
        for (UUID uuid : onlinePlayers) {
            if (!hasVotedToday(uuid)) {
                uuids.add(getVotes(uuid));
            }
        }
        return uuids;
    }

    @Override
    public void save() {
        Map<UUID, PlayerRecord> prs;
        rwl.readLock().lock();
        try {
            prs = new HashMap<>();
            for (Map.Entry<UUID, PlayerRecord> entry : voteCounts.entrySet()) {
                prs.put(entry.getKey(), entry.getValue().copy());
            }
        } finally {
            rwl.readLock().unlock();
        }

        // Save to a temporary file and then copy over the existing file.
        try {
            Path tempPath = Files.createTempFile("superbvote-", ".json");
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardOpenOption.WRITE)) {
                gson.toJson(new VotingFile(VERSION, prs), writer);
            }

            Files.move(tempPath, saveTo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save votes to " + saveTo, e);
        }
    }

    @Override
    public void close() {
        // No-op: onDisable already calls save() for us.
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
        private String lastKnownUsername;
        private int votes;
        private long lastVoted;

        private PlayerRecord(int votes) {
            this(votes, System.currentTimeMillis());
        }

        private PlayerRecord(int votes, long lastVoted) {
            this(null, votes, lastVoted);
        }

        public PlayerRecord(String lastKnownUsername, int votes, long lastVoted) {
            this.lastKnownUsername = lastKnownUsername;
            this.votes = votes;
            this.lastVoted = lastVoted;
        }

        public PlayerRecord copy() {
            return new PlayerRecord(lastKnownUsername, votes, lastVoted);
        }

        @Override
        public int compareTo(PlayerRecord o) {
            return Integer.compare(votes, o.votes);
        }
    }
}
