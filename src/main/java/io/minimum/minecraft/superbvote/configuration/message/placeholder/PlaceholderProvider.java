package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PlaceholderProvider {
    String apply(String message, MessageContext context);

    boolean canUse();

    boolean canUseForOfflinePlayers();
}
