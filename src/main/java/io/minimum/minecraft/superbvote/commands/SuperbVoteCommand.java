package io.minimum.minecraft.superbvote.commands;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.TextLeaderboardConfiguration;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.migration.GAListenerMigration;
import io.minimum.minecraft.superbvote.migration.Migration;
import io.minimum.minecraft.superbvote.migration.ProgressListener;
import io.minimum.minecraft.superbvote.migration.SuperbVoteJsonFileMigration;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SuperbVoteCommand implements CommandExecutor {
    public static final String FAKE_HOST_NAME_FOR_VOTE = UUID.randomUUID().toString();
    private final Map<String, ConfirmingCommand> wantToClear = new HashMap<>();

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "      " +
                ChatColor.GRAY + " SuperbVote " +
                ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "      ");

        sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv votes [player]");
        sender.sendMessage(ChatColor.GRAY + "Checks your vote amount, or the specified player's.");

        if (sender.hasPermission("superbvote.top") || sender.hasPermission("superbvote.admin")) {
            sender.sendMessage(ChatColor.GRAY + ChatColor.BOLD.toString() + "/sv top [page]");
            sender.sendMessage(ChatColor.GRAY + "Shows the top players on the voting leaderboard.");
        }

        if (sender.hasPermission("superbvote.admin")) {
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
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            // Nag, except on /sv reload.
            if (!sender.hasPermission("superbvote.admin") || !(args.length == 1 && args[0].equals("reload"))) {
                BrokenNag.nag(sender);
                return true;
            }
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0]) {
            case "votes":
                boolean canViewOthersVotes = sender.hasPermission("superbvote.admin") ||
                        sender.hasPermission("superbvote.votes.others");
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    UUID uuid;
                    String name;
                    if (args.length == 1) {
                        if (sender instanceof Player) {
                            uuid = ((Player) sender).getUniqueId();
                            name = sender.getName();
                        } else {
                            sender.sendMessage(ChatColor.RED + "You can't do this unless you're a player!");
                            return;
                        }
                    } else if (args.length == 2) {
                        if (!canViewOthersVotes) {
                            sender.sendMessage(ChatColor.RED + "You can't do this.");
                            return;
                        }
                        uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                        name = args[1];
                    } else {
                        sender.sendMessage(ChatColor.RED + "Need to specify at most one argument.");
                        sender.sendMessage(ChatColor.RED + "/sv votes [player]");
                        sender.sendMessage(ChatColor.RED + "Checks your vote amount, or the specified player's.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + name + " has " + SuperbVote.getPlugin().getVoteStorage().getVotes(uuid).getVotes() + " votes.");
                });
                return true;
            case "top":
                if (!(sender.hasPermission("superbvote.admin") || sender.hasPermission("superbvote.top"))) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                if (args.length > 2) {
                    sender.sendMessage(ChatColor.RED + "Need to specify at most one argument.");
                    sender.sendMessage(ChatColor.RED + "/sv top [page]");
                    sender.sendMessage(ChatColor.RED + "Shows the top players on the voting leaderboard.");
                    return true;
                }
                int page;
                try {
                    page = args.length == 2 ? Integer.parseInt(args[1]) - 1 : 0;
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Page number is not valid.");
                    return true;
                }

                if (page < 0) {
                    sender.sendMessage(ChatColor.RED + "Page number is not valid.");
                    return true;
                }

                String format = !(sender instanceof Player) || page > 0 ? "text" :
                        SuperbVote.getPlugin().getConfig().getString("leaderboard.display", "text");

                switch (format) {
                    case "text":
                    default:
                        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                            TextLeaderboardConfiguration config = SuperbVote.getPlugin().getConfiguration().getTextLeaderboardConfiguration();
                            int c = config.getPerPage();
                            int from = c * page;
                            List<PlayerVotes> leaderboard = SuperbVote.getPlugin().getVoteStorage().getTopVoters(c, page);
                            if (leaderboard.isEmpty()) {
                                sender.sendMessage(ChatColor.RED + "No entries found.");
                                return;
                            }
                            sender.sendMessage(config.getHeader().getBaseMessage());
                            for (int i = 0; i < leaderboard.size(); i++) {
                                String posStr = Integer.toString(from + i + 1);
                                sender.sendMessage(config
                                        .getEntryText()
                                        .getWithOfflinePlayer(sender, new MessageContext(null, leaderboard.get(i), null, null))
                                        .replaceAll("%num%", posStr));
                            }
                            int availablePages = SuperbVote.getPlugin().getVoteStorage().getPagesAvailable(c);
                            sender.sendMessage(config
                                    .getPageNumberText()
                                    .getBaseMessage()
                                    .replaceAll("%page%", Integer.toString(page + 1))
                                    .replaceAll("%total%", Integer.toString(availablePages)));
                        });
                        break;
                    case "scoreboard":
                        SuperbVote.getPlugin().getScoreboardHandler().toggle((Player) sender);
                        break;
                }

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

                com.vexsoftware.votifier.model.Vote vote = new com.vexsoftware.votifier.model.Vote();
                vote.setUsername(args[1]);
                vote.setTimeStamp(new Date().toString());
                vote.setAddress(FAKE_HOST_NAME_FOR_VOTE);
                vote.setServiceName(args[2]);
                Bukkit.getPluginManager().callEvent(new VotifierEvent(vote));

                sender.sendMessage(ChatColor.GREEN + "You have created a fake vote for " + player.getName() + ".");
                break;
            case "reload":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                SuperbVote.getPlugin().reloadPlugin();
                if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
                    sender.sendMessage(ChatColor.YELLOW + "Plugin configuration reloaded, but a configuration error was found.");
                    sender.sendMessage(ChatColor.YELLOW + "Please check the console for more details.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Plugin configuration reloaded.");
                }
                return true;
            case "clear":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }

                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_RED + ChatColor.BOLD.toString() + "DANGER DANGER DANGER DANGER DANGER DANGER");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.RED + "This command will " + ChatColor.BOLD + "irreversibly" + ChatColor.RESET + ChatColor.RED + " clear all your server's votes!");
                sender.sendMessage(ChatColor.RED + "If you want to continue, use the command /sv reallyclear in the next 15 seconds.");
                sender.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "You have been warned.");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_RED + ChatColor.BOLD.toString() + "DANGER DANGER DANGER DANGER DANGER DANGER");
                sender.sendMessage("");

                final String name = sender.getName();
                BukkitTask task = Bukkit.getScheduler().runTaskLater(SuperbVote.getPlugin(), () -> wantToClear.remove(name), 15 * 20);
                wantToClear.put(sender.getName(), new ConfirmingCommand(task));

                return true;
            case "reallyclear":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                ConfirmingCommand confirm1 = wantToClear.remove(sender.getName());
                if (confirm1 != null) {
                    confirm1.getCancellationTask().cancel();
                    SuperbVote.getPlugin().getVoteStorage().clearVotes();
                    SuperbVote.getPlugin().getQueuedVotes().clearVotes();

                    Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                        SuperbVote.getPlugin().getScoreboardHandler().doPopulate();
                        new TopPlayerSignFetcher(SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()).run();
                    });

                    sender.sendMessage(ChatColor.GREEN + "All votes cleared from the database.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You took a wrong turn. Try again using /sv clear.");
                }

                return true;
            case "migrate":
                if (!sender.hasPermission("superbvote.admin")) {
                    sender.sendMessage(ChatColor.RED + "You can't do this.");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Need to specify an argument.");
                    sender.sendMessage(ChatColor.RED + "/sv migrate <gal|svjson>");
                    sender.sendMessage(ChatColor.RED + "Migrate votes from another vote plugin.");
                    return true;
                }
                Migration migration;
                switch (args[1]) {
                    case "gal":
                        migration = new GAListenerMigration();
                        break;
                    case "svjson":
                        migration = new SuperbVoteJsonFileMigration();
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Not a valid listener. Currently supported: gal, svjson.");
                        return true;
                }
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
                    if (SuperbVote.getPlugin().getVoteStorage().getPagesAvailable(1) > 0) {
                        sender.sendMessage(ChatColor.RED + "You already have votes in the database. Use /sv clear and try again.");
                        return;
                    }
                    try {
                        sender.sendMessage(ChatColor.GRAY + "Migrating... (you can check the progress in the console)");
                        migration.execute(new ProgressListener() {
                            @Override
                            public void onStart(int records) {
                                SuperbVote.getPlugin().getLogger().info("Converting " + records + " records from " + migration.getName() + " to SuperbVote...");
                            }

                            @Override
                            public void onRecordBatch(int num, int total) {
                                String percentage = BigDecimal.valueOf(num)
                                        .divide(BigDecimal.valueOf(total), BigDecimal.ROUND_HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(1, BigDecimal.ROUND_HALF_UP)
                                        .toPlainString();
                                SuperbVote.getPlugin().getLogger().info("Converted " + num + " records to SuperbVote... (" + percentage + "% complete)");
                            }

                            @Override
                            public void onFinish(int records) {
                                SuperbVote.getPlugin().getLogger().info("Successfully converted all " + records + " records to SuperbVote!");

                                SuperbVote.getPlugin().getScoreboardHandler().doPopulate();
                                new TopPlayerSignFetcher(SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()).run();
                            }
                        });
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

    @Data
    private class ConfirmingCommand {
        private final BukkitTask cancellationTask;
    }
}
