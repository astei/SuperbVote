package io.minimum.minecraft.superbvote.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class SuperbVoteListener implements Listener {
    @EventHandler
    public void onVote(final VotifierEvent event) {
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            SuperbVote.getPlugin().getLogger().severe("Refusing to process vote because your configuration is invalid. Please check your logs.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(event.getVote().getUsername());
            String worldName = null;
            if (op.isOnline()) {
                worldName = op.getPlayer().getWorld().getName();
            }

            VoteStorage voteStorage = SuperbVote.getPlugin().getVoteStorage();
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(op.getUniqueId(), false);
            PlayerVotes pvCurrent = voteStorage.getVotes(op.getUniqueId());
            PlayerVotes pv = new PlayerVotes(op.getUniqueId(), op.getName(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);
            Vote vote = new Vote(op.getName(), op.getUniqueId(), event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVote.getPlugin().getConfiguration().getStreaksConfiguration().isSharedCooldownPerService()) {
                    if (voteStreak == null) {
                        // becomes a required value
                        voteStreak = voteStorage.getVoteStreakIfSupported(op.getUniqueId(), true);
                    }
                    if (voteStreak != null && voteStreak.getServices().containsKey(vote.getServiceName())) {
                        long difference = SuperbVote.getPlugin().getVoteServiceCooldown().getMax() - voteStreak.getServices().get(vote.getServiceName());
                        if (difference > 0) {
                            SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                                    vote.getServiceName() + ") due to [shared] service cooldown.");
                            return;
                        }
                    }
                }

                if (SuperbVote.getPlugin().getVoteServiceCooldown().triggerCooldown(vote)) {
                    SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                            vote.getServiceName() + ") due to service cooldown.");
                    return;
                }
            }

            processVote(pv, voteStreak, vote, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    !op.isOnline() && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline(),
                    false);
        });
    }

    private void processVote(PlayerVotes pv, VoteStreak voteStreak, Vote vote, boolean broadcast, boolean queue, boolean wasQueued) {
        List<VoteReward> bestRewards = SuperbVote.getPlugin().getConfiguration().getBestRewards(vote, pv);
        MessageContext context = new MessageContext(vote, pv, voteStreak, Bukkit.getOfflinePlayer(vote.getUuid()));
        boolean canBroadcast = SuperbVote.getPlugin().getRecentVotesStorage().canBroadcast(vote.getUuid());
        SuperbVote.getPlugin().getRecentVotesStorage().updateLastVote(vote.getUuid());

        Optional<Player> player = context.getPlayer().map(OfflinePlayer::getPlayer);
        boolean hideBroadcast = player.isPresent() && player.get().hasPermission("superbvote.bypassbroadcast");

        // Get the plugin config instance
        FileConfiguration config = SuperbVote.getPlugin().getConfig();

        // Get the current vote count
        int votes = config.getInt("votes.globalVotes");

        // Increment the vote count
        votes++;

        // Update the vote count in the config
        config.set("votes.globalVotes", votes);

        // Get the current vote goal
        int goal = config.getInt("votes.voteGoal");

        // Check if the vote goal has been reached
        if (votes >= goal) {
            // Broadcast the goal reached message
            for (String text : config.getStringList("votes.goalText")) {
                Bukkit.broadcastMessage(text);
            }

            // Double the vote goal
            goal *= config.getInt("votes.multiply");
            // Update the goal in the config
            config.set("votes.voteGoal", goal);

            // Reset the vote count
            config.set("votes.globalVotes", 0);

            // Execute the commands in the config
            for (String command : config.getStringList("votes.commandGoal")) {
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
            }
        } else {
            // Broadcast the vote message
            for (String text : config.getStringList("votes.voteText")) {
                text = text.replace("%votes%", String.valueOf(votes)).replace("%goal%", String.valueOf(goal));
                Bukkit.broadcastMessage(text);
            }
        }

        // Save the config
        SuperbVote.getPlugin().saveConfig();

        if (bestRewards.isEmpty()) {
            throw new RuntimeException("No vote rewards found for '" + vote + "'");
        }

        if (queue) {
            if (!SuperbVote.getPlugin().getConfiguration().shouldQueueVotes()) {
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                        vote.getServiceName() + ") because they aren't online.");
                return;
            }

            SuperbVote.getPlugin().getLogger().log(Level.INFO, "Queuing vote from " + vote.getName() + " to be run later");
            for (VoteReward reward : bestRewards) {
                reward.broadcastVote(context, false, broadcast && SuperbVote.getPlugin().getConfig().getBoolean("broadcast.queued") && canBroadcast && !hideBroadcast);
            }
            SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
        } else {
            if (!vote.isFakeVote() || SuperbVote.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                SuperbVote.getPlugin().getVoteStorage().addVote(vote);
            }

            if (!wasQueued) {
                for (VoteReward reward : bestRewards) {
                    reward.broadcastVote(context, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.message-player"), broadcast && canBroadcast && !hideBroadcast);
                }
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), this::afterVoteProcessing);
            }

            Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> bestRewards.forEach(reward -> reward.runCommands(vote)));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            if (event.getPlayer().hasPermission("superbvote.admin")) {
                Player player = event.getPlayer();
                Bukkit.getScheduler().runTaskLater(SuperbVote.getPlugin(), () -> BrokenNag.nag(player), 40);
            }
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            // Update names in MySQL, if it is being used.
            if (SuperbVote.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVote.getPlugin().getVoteStorage()).updateName(event.getPlayer());
            }

            // Process queued votes.
            VoteStorage voteStorage = SuperbVote.getPlugin().getVoteStorage();
            UUID playerUUID = event.getPlayer().getUniqueId();
            PlayerVotes pv = voteStorage.getVotes(playerUUID);
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(playerUUID, false);
            List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(playerUUID);
            if (!votes.isEmpty()) {
                for (Vote vote : votes) {
                    processVote(pv, voteStreak, vote, false, false, true);
                    pv = new PlayerVotes(pv.getUuid(), event.getPlayer().getName(), pv.getVotes() + 1, PlayerVotes.Type.CURRENT);
                }
                afterVoteProcessing();
            }

            // Remind players to vote.
            if (SuperbVote.getPlugin().getConfig().getBoolean("vote-reminder.on-join") &&
                    event.getPlayer().hasPermission("superbvote.notify") &&
                    !SuperbVote.getPlugin().getVoteStorage().hasVotedToday(event.getPlayer().getUniqueId())) {
                MessageContext context = new MessageContext(null, pv, voteStreak, event.getPlayer());
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(event.getPlayer(), context);
            }
        });
    }

    private void afterVoteProcessing() {
        SuperbVote.getPlugin().getScoreboardHandler().doPopulate();
        new TopPlayerSignFetcher(SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()).run();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            SuperbVote.getPlugin().getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }
    }
}
