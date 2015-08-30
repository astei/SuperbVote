package io.minimum.minecraft.superbvote.handler;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.rewards.VoteReward;
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
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
            } else {
                uuid = SuperbVote.getPlugin().getUuidCache().getUuidFromName(event.getVote().getUsername());
            }

            if (uuid == null) {
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + event.getVote().getUsername() + " as we couldn't look up their username");
                return;
            }

            Vote vote = new Vote(event.getVote().getUsername(), uuid, event.getVote().getServiceName(), new Date());

            if (SuperbVote.getPlugin().getConfiguration().requirePlayersOnline()) {
                SuperbVote.getPlugin().getVoteStorage().addVote(uuid);
                SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Queuing vote from " + event.getVote().getUsername() + " to be run later (player not online)");
                return;
            }

            processVote(vote, true);
        });
    }

    private void processVote(Vote vote, boolean broadcast) {
        VoteReward bestReward = SuperbVote.getPlugin().getConfiguration().getBestReward(vote);
        SuperbPreVoteEvent preVoteEvent = new SuperbPreVoteEvent(vote);
        preVoteEvent.setVoteReward(bestReward);
        Bukkit.getPluginManager().callEvent(preVoteEvent);

        switch (preVoteEvent.getResult()) {
            case PROCESS_VOTE:
                if (preVoteEvent.getVoteReward() == null) {
                    throw new RuntimeException("No vote reward found for '" + vote + "'");
                }

                if (broadcast) preVoteEvent.getVoteReward().broadcastVote(vote);

                SuperbVote.getPlugin().getVoteStorage().addVote(vote.getUuid());
                Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> {
                    Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote, preVoteEvent.getVoteReward()));
                });
                break;
            case QUEUE_VOTE:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Queuing vote from " + vote.getName() + " to be run later (event)");
                SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
                break;
            case CANCEL:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Vote from " + vote.getName() + " cancelled (event)");
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SuperbVote.getPlugin().getUuidCache().cachePlayer(event.getPlayer());
        List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> votes.forEach(v -> processVote(v, false)));
    }
}
