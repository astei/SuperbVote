package io.minimum.minecraft.superbvote.scoreboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScoreboardHandler implements Runnable {
    private final Scoreboard scoreboard;
    private final Objective objective;

    public ScoreboardHandler() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(
                ChatColor.translateAlternateColorCodes('&',
                        SuperbVote.getPlugin().getConfig().getString("leaderboard.scoreboard.title", "Top voters")),
                "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore("None found").setScore(1);
    }

    @Override
    public void run() {
        List<UUID> leaderboardAsUuids = SuperbVote.getPlugin().getVoteStorage().getTopVoters(
                Math.min(16, SuperbVote.getPlugin().getConfig().getInt("leaderboard.scoreboard.max", 10)), 0);
        List<String> leaderboard = leaderboardAsUuids.stream()
                .map(uuid -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(uuid))
                .collect(Collectors.toList());
        if (leaderboard.isEmpty()) {
            scoreboard.getEntries().stream().filter(s -> !s.equals("None found")).forEach(scoreboard::resetScores);
            objective.getScore("None found").setScore(1);
        } else {
            scoreboard.getEntries().stream().filter(s -> !leaderboard.contains(s)).forEach(scoreboard::resetScores);
            for (UUID uuid : leaderboardAsUuids) {
                int votes = SuperbVote.getPlugin().getVoteStorage().getVotes(uuid);
                String name = SuperbVote.getPlugin().getUuidCache().getNameFromUuid(uuid);
                if (name == null) continue;

                objective.getScore(name).setScore(votes);
            }
        }
    }

    public void toggle(Player player) {
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }
}
