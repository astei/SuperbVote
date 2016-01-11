package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TopPlayerSignFetcher implements Runnable {
    private final List<TopPlayerSign> toUpdate;

    @Override
    public void run() {
        // Determine how many players to fetch from the leaderboard.
        OptionalInt toFetch = toUpdate.stream().mapToInt(TopPlayerSign::getPosition).max();
        if (!toFetch.isPresent()) {
            return;
        }

        // Fetch the players required.
        List<UUID> topPlayers = SuperbVote.getPlugin().getVoteStorage().getTopVoters(toFetch.getAsInt(), 0);

        // We've done everything we can do asynchronously. Hand off to the synchronous update task.
        Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), new TopPlayerSignUpdater(toUpdate, topPlayers));
    }
}
