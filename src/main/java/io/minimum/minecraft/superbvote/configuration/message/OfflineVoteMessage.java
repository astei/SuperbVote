package io.minimum.minecraft.superbvote.configuration.message;

import org.bukkit.command.CommandSender;

import java.util.UUID;

public interface OfflineVoteMessage {
    void sendWithNothing(CommandSender to);

    String getWithOfflinePlayer(CommandSender to, UUID referred);
}
