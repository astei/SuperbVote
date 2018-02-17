package io.minimum.minecraft.superbvote.configuration.message;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class JsonTextMessage extends MessageBase implements VoteMessage {
    private final String message;

    public JsonTextMessage(String message) {
        this.message = message;
    }

    @Override
    public void sendAsBroadcast(Player player, MessageContext context) {
        String jsonString = getAsBroadcast(message, context);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonString);
    }

    @Override
    public void sendAsReminder(Player player, MessageContext context) {
        String jsonString = getAsReminder(message, context);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonString);
    }
}
