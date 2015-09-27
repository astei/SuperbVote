package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

public interface VoteMessage {
    void sendAsBroadcast(Player player, Vote vote);

    void sendAsReminder(Player player);
}
