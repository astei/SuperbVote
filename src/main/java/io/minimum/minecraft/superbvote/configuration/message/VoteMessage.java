package io.minimum.minecraft.superbvote.configuration.message;

import org.bukkit.entity.Player;

public interface VoteMessage {
    void sendAsBroadcast(Player player, MessageContext context);

    void sendAsReminder(Player player, MessageContext context);
}
