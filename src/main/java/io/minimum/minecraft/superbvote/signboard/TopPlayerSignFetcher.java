package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.OptionalInt;
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
        List<String> topPlayers = SuperbVote.getPlugin().getVoteStorage().getTopVoters(toFetch.getAsInt(), 0).stream()
                .map(uuid -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(uuid))
                .collect(Collectors.toList());

        // We've done everything we can do asynchronously. Hand off to the synchronous update task.
        Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), new TopPlayerSignUpdater(toUpdate, topPlayers));
    }
}
