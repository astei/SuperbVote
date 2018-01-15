package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class JsonTextMessage extends MessageBase implements VoteMessage {
    private final String message;

    public JsonTextMessage(String message) {
        this.message = message;
    }

    @Override
    public void sendAsBroadcast(Player player, Vote vote) {
        String jsonString = getAsBroadcast(message, vote);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonString);
    }

    @Override
    public void sendAsReminder(Player player) {
        String jsonString = getAsReminder(message, player);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonString);
    }
}
