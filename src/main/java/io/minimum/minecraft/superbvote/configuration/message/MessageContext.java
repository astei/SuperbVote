package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

public class MessageContext {
    private final Vote vote;
    private final PlayerVotes voteRecord;
    private final OfflinePlayer player;

    public MessageContext(Vote vote, PlayerVotes voteRecord, OfflinePlayer player) {
        this.vote = vote;
        this.voteRecord = voteRecord;
        this.player = player;
    }

    public Optional<Vote> getVote() {
        return Optional.ofNullable(vote);
    }

    public PlayerVotes getVoteRecord() {
        return voteRecord;
    }

    public Optional<OfflinePlayer> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public String toString() {
        return "MessageContext{" +
                "vote=" + vote +
                ", voteRecord=" + voteRecord +
                ", player=" + player +
                '}';
    }
}
