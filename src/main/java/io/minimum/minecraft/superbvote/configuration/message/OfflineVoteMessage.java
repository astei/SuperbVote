package io.minimum.minecraft.superbvote.configuration.message;

import org.bukkit.command.CommandSender;

public interface OfflineVoteMessage {
    void sendWithNothing(CommandSender to);

    String getWithOfflinePlayer(CommandSender to, MessageContext context);
}
