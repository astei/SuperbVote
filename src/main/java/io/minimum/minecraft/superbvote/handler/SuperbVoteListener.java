package io.minimum.minecraft.superbvote.handler;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.VoteService;
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

            VoteService service = SuperbVote.getPlugin().getConfiguration().getService(event.getVote().getServiceName());

            if (service == null) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "No vote section found");
                return;
            }

            Vote vote = new Vote(event.getVote().getUsername(), uuid, service, event.getVote().getServiceName(), new Date());
            SuperbPreVoteEvent preVoteEvent = new SuperbPreVoteEvent(vote);
            Bukkit.getPluginManager().callEvent(preVoteEvent);
            if (preVoteEvent.isCancelled()) {
                return; // don't bother processing
            }

            SuperbVote.getPlugin().getVoteStorage().addVote(uuid);

            if (onlinePlayer != null && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline()) {
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Queuing vote from " + event.getVote().getUsername() + " to be run later as they are not online");
                SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
                return;
            }

            Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote)));
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SuperbVote.getPlugin().getUuidCache().cachePlayer(event.getPlayer());
        List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(event.getPlayer().getUniqueId());
        votes.forEach(vote -> {
            vote.getService().runCommands(vote);
        });
    }
}
