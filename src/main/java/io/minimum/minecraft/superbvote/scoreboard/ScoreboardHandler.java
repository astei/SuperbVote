package io.minimum.minecraft.superbvote.scoreboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardHandler {
    private final Scoreboard scoreboard;
    private final Objective objective;

    public ScoreboardHandler() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("", "dummy");
        reload();
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore("None found").setScore(1);
    }

    public void reload() {
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                SuperbVote.getPlugin().getConfig().getString("leaderboard.scoreboard.title", "Top voters")));
    }

    public void doPopulate() {
        if (!SuperbVote.getPlugin().getConfig().getString("leaderboard.display").equals("scoreboard")) {
            return;
        }
        List<PlayerVotes> leaderboardAsUuids = SuperbVote.getPlugin().getVoteStorage().getTopVoters(
                Math.min(16, SuperbVote.getPlugin().getConfig().getInt("leaderboard.scoreboard.max", 10)), 0);
        List<String> leaderboardAsNames = leaderboardAsUuids.stream()
                .map(ue -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(ue.getUuid()))
                .collect(Collectors.toList());
        if (leaderboardAsNames.isEmpty()) {
            scoreboard.getEntries().stream().filter(s -> !s.equals("None found")).forEach(scoreboard::resetScores);
            objective.getScore("None found").setScore(1);
        } else {
            scoreboard.getEntries().stream().filter(s -> !leaderboardAsNames.contains(s)).forEach(scoreboard::resetScores);
            for (int i = 0; i < leaderboardAsUuids.size(); i++) {
                PlayerVotes e = leaderboardAsUuids.get(i);
                String name = leaderboardAsNames.get(i);
                objective.getScore(name).setScore(e.getVotes());
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
