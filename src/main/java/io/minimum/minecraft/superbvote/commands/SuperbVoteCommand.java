package io.minimum.minecraft.superbvote.commands;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.migration.GAListenerMigration;
import io.minimum.minecraft.superbvote.migration.Migration;
import io.minimum.minecraft.superbvote.votes.SuperbPreVoteEvent;
import io.minimum.minecraft.superbvote.votes.SuperbVoteEvent;
import io.minimum.minecraft.superbvote.votes.Vote;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
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

            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv migrate <gal>");
            sender.sendMessage(ChatColor.GRAY + "Migrate votes from another vote plugin.");

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

                String format = !(sender instanceof Player) ? "text" :
                        SuperbVote.getPlugin().getConfig().getString("leaderboard.display", "text");

                switch (format) {
                    case "text":
                    default:
                        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                            int c = SuperbVote.getPlugin().getConfig().getInt("leaderboard.text.per-page", 10);
                            int from = c * page;
                            List<UUID> leaderboardAsUuids = SuperbVote.getPlugin().getVoteStorage().getTopVoters(c, page);
                            List<String> leaderboard = leaderboardAsUuids.stream()
                                    .map(uuid -> SuperbVote.getPlugin().getUuidCache().getNameFromUuid(uuid))
                                    .collect(Collectors.toList());
                            if (leaderboardAsUuids.isEmpty()) {
                                sender.sendMessage(ChatColor.RED + "No entries found.");
                                return;
                            }
                            sender.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "      " +
                                    ChatColor.GRAY + " Top Players " +
                                    ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "      ");
                            for (int i = 0; i < leaderboard.size(); i++) {
                                sender.sendMessage(ChatColor.GRAY + Integer.toString(from + i + 1) + ". " + ChatColor.YELLOW + leaderboard.get(i));
                            }
                            int availablePages = SuperbVote.getPlugin().getVoteStorage().getPagesAvailable(10);
                            sender.sendMessage(ChatColor.GRAY + "(page " + (page + 1) + "/" + availablePages + ")");
                        });
                        break;
                    case "scoreboard":
                        SuperbVote.getPlugin().getScoreboardHandler().toggle((Player) sender);
                        break;
                }

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

                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    Vote vote = new Vote(player.getName(), player.getUniqueId(), args[2], new Date());
                    VoteReward bestReward = SuperbVote.getPlugin().getConfiguration().getBestReward(vote);
                    SuperbPreVoteEvent preVoteEvent = new SuperbPreVoteEvent(vote);
                    preVoteEvent.setVoteReward(bestReward);
                    Bukkit.getPluginManager().callEvent(preVoteEvent);

                    if (preVoteEvent.getResult() == SuperbPreVoteEvent.Result.PROCESS_VOTE) {
                        Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> {
                            Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote, preVoteEvent.getVoteReward()));
                            sender.sendMessage(ChatColor.GREEN + "You have created a fake vote for " + player.getName() + ".");
                        });
                    } else if (preVoteEvent.getResult() == SuperbPreVoteEvent.Result.QUEUE_VOTE) {
                        sender.sendMessage(ChatColor.RED + "The fake vote for " + player.getName() + " would be queued. Bailing.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "The fake vote for " + player.getName() + " was cancelled.");
                    }
                });
                break;
            case "reload":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                SuperbVote.getPlugin().reloadPlugin();
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
            case "migrate":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Need to specify an argument.");
                    sender.sendMessage(ChatColor.RED + "/sv migrate <gal>");
                    sender.sendMessage(ChatColor.RED + "Migrate votes from another vote plugin.");
                    return true;
                }
                Migration migration;
                switch (args[0]) {
                    case "gal":
                        migration = new GAListenerMigration();
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Not a valid listener. Currently supported: gal.");
                        return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Migrating... (this may take several minutes)");
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    try {
                        migration.execute();
                        sender.sendMessage(ChatColor.GREEN + "Migration succeeded!");
                    } catch (Exception e) {
                        SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to migrate", e);
                        sender.sendMessage(ChatColor.RED + "Migration failed. Check the console for details.");
                    }
                });
                return true;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }
}
