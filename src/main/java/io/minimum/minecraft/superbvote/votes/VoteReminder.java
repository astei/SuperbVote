package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.storage.ExtendedVoteStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoteReminder implements Runnable {
    @Override
    public void run() {
        List<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("superbvote.notify")).map(Player::getUniqueId).collect(Collectors.toList());

        VoteStorage voteStorage = SuperbVote.getPlugin().getVoteStorage();
        if (SuperbVote.getPlugin().getConfiguration().getStreaksConfiguration().isPlaceholdersEnabled() && voteStorage instanceof ExtendedVoteStorage) {
            List<Map.Entry<PlayerVotes, VoteStreak>> noVotes = ((ExtendedVoteStorage) voteStorage).getAllPlayersAndStreaksWithNoVotesToday(onlinePlayers);
            for (Map.Entry<PlayerVotes, VoteStreak> entry : noVotes) {
                PlayerVotes pv = entry.getKey();
                VoteStreak voteStreak = entry.getValue();

                Player player = Bukkit.getPlayer(pv.getUuid());
                if (player != null) {
                    MessageContext context = new MessageContext(null, pv, voteStreak, player);
                    SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(player, context);
                }
            }
        } else {
            List<PlayerVotes> noVotes = voteStorage.getAllPlayersWithNoVotesToday(onlinePlayers);
            for (PlayerVotes pv : noVotes) {
                Player player = Bukkit.getPlayer(pv.getUuid());
                if (player != null) {
                    MessageContext context = new MessageContext(null, pv, null, player);
                    SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(player, context);
                }
            }
        }
    }
}
