package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.VoteStreak;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

public class MessageContext {
    private final Vote vote;
    private final PlayerVotes voteRecord;
    private final VoteStreak streakRecord;
    private final OfflinePlayer player;

    public MessageContext(Vote vote, PlayerVotes voteRecord, VoteStreak streakRecord, OfflinePlayer player) {
        this.vote = vote;
        this.voteRecord = voteRecord;
        this.streakRecord = streakRecord;
        this.player = player;
    }

    public Optional<Vote> getVote() {
        return Optional.ofNullable(vote);
    }

    public PlayerVotes getVoteRecord() {
        return voteRecord;
    }

    public Optional<VoteStreak> getStreakRecord() {
        return Optional.ofNullable(streakRecord);
    }

    public Optional<OfflinePlayer> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public String toString() {
        return "MessageContext{" +
                "vote=" + vote +
                ", voteRecord=" + voteRecord +
                ", streakRecord=" + streakRecord +
                ", player=" + player +
                '}';
    }
}
