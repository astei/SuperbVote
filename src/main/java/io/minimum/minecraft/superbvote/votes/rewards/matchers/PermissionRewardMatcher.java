package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@ToString
public class PermissionRewardMatcher implements RewardMatcher {
    private final String permission;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        Player player = Bukkit.getPlayer(vote.getUuid());
        return player != null && player.hasPermission(permission);
    }
}
