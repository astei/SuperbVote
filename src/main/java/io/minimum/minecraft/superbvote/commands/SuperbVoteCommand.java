package io.minimum.minecraft.superbvote.commands;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.VoteService;
import io.minimum.minecraft.superbvote.handler.SuperbVoteEvent;
import io.minimum.minecraft.superbvote.handler.Vote;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SuperbVoteCommand implements CommandExecutor {
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "      " +
                ChatColor.GRAY + " SuperbVote " +
                ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "      ");

        sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv votes [player]");
        sender.sendMessage(ChatColor.GRAY + "Checks your vote amount, or the specified player's.");

        if (sender.hasPermission("superbvote.top") || sender.hasPermission("superbvote.admin")) {
            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv top [page]");
            sender.sendMessage(ChatColor.GRAY + "Shows the top players.");
        }

        if (sender.hasPermission("superbvote.admin")) {
            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv pastetop <amount>");
            sender.sendMessage(ChatColor.GRAY + "Pastes the top [amount] players.");

            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv fakevote <player> [service]");
            sender.sendMessage(ChatColor.GRAY + "Issues a fake vote for the specified player.");

            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv reload");
            sender.sendMessage(ChatColor.GRAY + "Reloads the plugin's configuration.");

            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv clear");
            sender.sendMessage(ChatColor.GRAY + "Clears all stored and queued votes.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0]) {
            case "votes":
                boolean isAdmin = sender.hasPermission("superbvote.admin");
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    UUID uuid;
                    String name;
                    if (args.length == 1) {
                        if (sender instanceof Player) {
                            uuid = ((Player) sender).getUniqueId();
                            name = sender.getName();
                        } else {
                            sender.sendMessage(ChatColor.RED + "You can't do this unless you're a player/");
                            return;
                        }
                    } else if (args.length == 2) {
                        if (!isAdmin) {
                            sender.sendMessage(ChatColor.RED + "You can't do this.");
                            return;
                        }
                        uuid = SuperbVote.getPlugin().getUuidCache().getUuidFromName(args[1]);
                        name = args[1];
                    } else {
                        sender.sendMessage(ChatColor.RED + "Need to specify at most one arguments.");
                        sender.sendMessage(ChatColor.RED + "/sv votes [player]");
                        sender.sendMessage(ChatColor.RED + "Checks your vote amount, or the specified player's.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + name + " has " + SuperbVote.getPlugin().getVoteStorage().getVotes(uuid) + " votes.");
                });
                return true;
            case "top":
                if (!sender.hasPermission("superbvote.admin") || !sender.hasPermission("superbvote.top")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                if (args.length > 2) {
                    sender.sendMessage(ChatColor.RED + "Need to specify at most one argument.");
                    sender.sendMessage(ChatColor.RED + "/sv top [page]");
                    sender.sendMessage(ChatColor.RED + "Shows the top players.");
                    return true;
                }
                int page;
                try {
                    page = args.length == 2 ? Integer.parseInt(args[1]) : 0;
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Page number is not valid.");
                    return true;
                }
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    List<UUID> leaderboardAsUuids = SuperbVote.getPlugin().getVoteStorage().getTopVoters(10, page);
                    if (leaderboardAsUuids.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "No entries found.");
                        return;
                    }
                    sender.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "      " +
                            ChatColor.GRAY + " Top Players " +
                            ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "      ");
                    List<String> leaderboard = leaderboardAsUuids.stream()
                            .map(leaderboardAsUuid -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(leaderboardAsUuid))
                            .collect(Collectors.toList());
                    for (int i = 0; i < leaderboard.size(); i++) {
                        sender.sendMessage(ChatColor.GRAY + Integer.toString(i + 1) + ". " + ChatColor.YELLOW + leaderboard.get(i));
                    }
                });
                return true;
            case "pastetop":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }

                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Need to specify an argument.");
                    sender.sendMessage(ChatColor.RED + "/sv pastetop <amount>");
                    sender.sendMessage(ChatColor.RED + "Pastes the top <amount> players.");
                    return true;
                }

                int amt;
                try {
                    amt = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Amount is not valid.");
                    return true;
                }

                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    List<UUID> leaderboardAsUuids = SuperbVote.getPlugin().getVoteStorage().getTopVoters(amt, 0);
                    if (leaderboardAsUuids.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "No entries found.");
                        return;
                    }
                    List<String> leaderboard = leaderboardAsUuids.stream()
                            .map(leaderboardAsUuid -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(leaderboardAsUuid))
                            .collect(Collectors.toList());
                    StringBuilder text = new StringBuilder();
                    for (int i = 0; i < leaderboard.size(); i++) {
                        text.append(i + 1).append(". ").append(leaderboard.get(i)).append('\n');
                    }
                    try {
                        String url = PasteSubmission.submitPaste(text.toString());
                        sender.sendMessage(ChatColor.GREEN + "Leaderboard pasted at " + url);
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Unable to paste the leaderboard.");
                        SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to paste the leaderboard", e);
                    }
                });
                return true;
            case "fakevote":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Need to specify two arguments.");
                    sender.sendMessage(ChatColor.RED + "/sv fakevote <player> <service>");
                    sender.sendMessage(ChatColor.RED + "Issues a fake vote for the specified player.");
                    return true;
                }

                Player player = Bukkit.getPlayer(args[1]);

                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "That player was not found.");
                    return true;
                }

                VoteService service = SuperbVote.getPlugin().getConfiguration().getService(args[2]);

                if (service == null) {
                    sender.sendMessage(ChatColor.RED + "That service is not valid.");
                    return true;
                }

                Vote vote = new Vote(player.getName(), player.getUniqueId(), service, args[2], new Date());

                service.broadcastVote(vote);
                Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote));

                sender.sendMessage(ChatColor.GREEN + "You have created a fake vote for " + player.getName() + ".");
                break;
            case "reload":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                SuperbVote.getPlugin().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Plugin configuration reloaded.");
                return true;
            case "clear":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                SuperbVote.getPlugin().getVoteStorage().clearVotes();
                SuperbVote.getPlugin().getQueuedVotes().clearVotes();
                sender.sendMessage(ChatColor.GREEN + "All votes cleared.");
                return true;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }
}
