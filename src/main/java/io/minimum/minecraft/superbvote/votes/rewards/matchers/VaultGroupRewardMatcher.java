package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class VaultGroupRewardMatcher implements RewardMatcher {
    private final Permission permission;
    private final List<String> groups;

    public VaultGroupRewardMatcher(Permission permission, List<String> groups) {
        this.permission = permission;
        this.groups = ImmutableList.copyOf(groups);
    }

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        List<String> playerGroups = getPlayerGroups(vote);
        return playerGroups.containsAll(groups);
    }

    private List<String> getPlayerGroups(Vote vote) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(vote.getUuid());
        if (op.isOnline()) {
            return ImmutableList.copyOf(permission.getPlayerGroups(op.getPlayer()));
        }
        // Otherwise, assume the first world loaded on this server. This can only be an educated guess.
        String firstWorld = Bukkit.getWorlds().get(0).getName();
        return ImmutableList.copyOf(permission.getPlayerGroups(firstWorld, op));
    }
}
