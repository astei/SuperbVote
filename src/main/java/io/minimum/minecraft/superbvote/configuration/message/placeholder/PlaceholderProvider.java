package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PlaceholderProvider {
    String applyForBroadcast(Player voted, String message, Vote vote);

    String applyForReminder(Player player, String message);

    boolean canUse();

    boolean canUseForOfflinePlayers();

    String applyForReminder(PlayerVotes player, String message);
}
