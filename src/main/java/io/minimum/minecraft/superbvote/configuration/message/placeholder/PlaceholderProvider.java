package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

public interface PlaceholderProvider {
    String applyForBroadcast(Player voted, String message, Vote vote);
    String applyForReminder(Player player, String message);
    boolean canUse();
}
