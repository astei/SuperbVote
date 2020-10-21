package io.minimum.minecraft.superbvote.configuration.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlainStringMessage extends MessageBase implements VoteMessage, OfflineVoteMessage {

    private final String message;

    public PlainStringMessage(String message) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void sendAsBroadcast(Player player, MessageContext context) {
        player.sendMessage(replace(message, context));
    }

    @Override
    public void sendAsReminder(Player player, MessageContext context) {
        player.sendMessage(replace(message, context));
    }

    @Override
    public String getBaseMessage() {
        return message;
    }

    @Override
    public String getWithOfflinePlayer(CommandSender to, MessageContext context) {
        return replace(message, context);
    }
}
