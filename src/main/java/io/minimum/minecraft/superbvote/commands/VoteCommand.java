package io.minimum.minecraft.superbvote.commands;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class VoteCommand implements CommandExecutor {
    private final VoteMessage message;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            BrokenNag.nag(player);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            MessageContext ctx = new MessageContext(null, SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId()), player);
            message.sendAsReminder(player, ctx);
        });
        return true;
    }
}
