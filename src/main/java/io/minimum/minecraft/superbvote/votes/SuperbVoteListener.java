package io.minimum.minecraft.superbvote.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SuperbVoteListener implements Listener {
    @EventHandler
    public void onVote(final VotifierEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            Player onlinePlayer = Bukkit.getPlayerExact(event.getVote().getUsername());
            UUID uuid;
            String caseCorrected;
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
                caseCorrected = onlinePlayer.getName();
            } else {
                // Permit case-correction during voting.
                uuid = SuperbVote.getPlugin().getUuidCache().getUuidFromName(event.getVote().getUsername());
                if (uuid == null) {
                    SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + event.getVote().getUsername() + " as we couldn't look up their UUID");
                    return;
                }
                caseCorrected = SuperbVote.getPlugin().getUuidCache().getNameFromUuid(uuid);
            }

            Vote vote = new Vote(caseCorrected, uuid, event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVote.getPlugin().getCooldownHandler().triggerCooldown(vote)) {
                    SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                            vote.getServiceName() + ") due to service cooldown.");
                    return;
                }
            }

            processVote(vote, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    onlinePlayer == null && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline(),
                    false);
        });
    }

    private void processVote(Vote vote, boolean broadcast, boolean queue, boolean queued) {
        List<VoteReward> bestRewards = SuperbVote.getPlugin().getConfiguration().getBestRewards(vote);
        SuperbPreVoteEvent preVoteEvent = new SuperbPreVoteEvent(vote, bestRewards);
        if (queue) {
            preVoteEvent.setResult(SuperbPreVoteEvent.Result.QUEUE_VOTE);
        }
        Bukkit.getPluginManager().callEvent(preVoteEvent);

        switch (preVoteEvent.getResult()) {
            case PROCESS_VOTE:
                if (preVoteEvent.getVoteRewards().isEmpty()) {
                    throw new RuntimeException("No vote reward found for '" + vote + "'");
                }

                if (!vote.isFakeVote() || SuperbVote.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                    SuperbVote.getPlugin().getVoteStorage().issueVote(vote);
                }

                for (VoteReward reward : preVoteEvent.getVoteRewards()) {
                    reward.broadcastVote(vote, !queued && SuperbVote.getPlugin().getConfig().getBoolean("broadcast.message-player"),
                            broadcast && !queued);
                }

                Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote, preVoteEvent.getVoteRewards())));
                break;
            case QUEUE_VOTE:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Queuing vote from " + vote.getName() + " to be run later");
                for (VoteReward reward : preVoteEvent.getVoteRewards()) {
                    reward.broadcastVote(vote, false, broadcast && SuperbVote.getPlugin().getConfig().getBoolean("broadcast.queued"));
                }
                SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
                break;
            case CANCEL:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Vote from " + vote.getName() + " cancelled (event)");
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            // Update names in MySQL, if it is being used.
            if (SuperbVote.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVote.getPlugin().getVoteStorage()).updateName(event.getPlayer());
            }

            // Process queued votes.
            List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(event.getPlayer().getUniqueId());
            votes.forEach(v -> processVote(v, false, false, true));

            // Remind players to vote.
            if (SuperbVote.getPlugin().getConfig().getBoolean("vote-reminder.on-join")) {
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(event.getPlayer());
            }
        });
    }
}
