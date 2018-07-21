package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.minimum.minecraft.superbvote.SuperbVote;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.logging.Level;

public class RewardMatchers {
    private static final Set<String> AVAILABLE_MATCHERS = ImmutableSet.<String>builder()
            .add("permission")
            .add("chance-fractional")
            .add("chance-percentage")
            .add("service")
            .add("services")
            .add("cumulative-votes")
            .add("every-cumulative-votes")
            .add("group")
            .add("groups")
            .add("world")
            .add("worlds")
            .add("script")
            .add("default")
            .build();
    private static final List<RewardMatcherFactory> MATCHER_FACTORIES = ImmutableList.<RewardMatcherFactory>builder()
            .add(ChanceFractionalRewardMatcher.FACTORY)
            .add(ChancePercentageRewardMatcher.FACTORY)
            .add(CumulativeVotesEveryRewardMatcher.FACTORY)
            .add(CumulativeVotesRewardMatcher.FACTORY)
            .add(PermissionRewardMatcher.FACTORY)
            .add(ScriptRewardMatcher.FACTORY)
            .add(ServiceRewardMatcher.FACTORY)
            .add(StaticRewardMatcher.DEFAULT_FACTORY)
            .add(VaultGroupRewardMatcher.FACTORY)
            .add(WorldRewardMatcher.FACTORY)
            .build();

    private static List<String> closestMatch(String string) {
        List<String> matched = new ArrayList<>();
        for (String availableMatcher : AVAILABLE_MATCHERS) {
            if (StringUtils.getLevenshteinDistance(string, availableMatcher) <= 2) {
                matched.add(availableMatcher);
            }
        }
        return matched;
    }

    public static List<RewardMatcher> getMatchers(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();

        // Before creating matchers, find any unknown matchers and try to help the user out.
        Set<String> unknownKeys = new HashSet<>(section.getKeys(false));
        unknownKeys.removeAll(AVAILABLE_MATCHERS);
        if (!unknownKeys.isEmpty()) {
            for (String key : unknownKeys) {
                List<String> closestMatches = closestMatch(key);
                if (closestMatches.size() == 1) {
                    SuperbVote.getPlugin().getLogger().warning("Unknown matcher '" + key + "'. Perhaps you meant '" + closestMatches.get(0) + "'?");
                } else if (closestMatches.size() >= 1){
                    SuperbVote.getPlugin().getLogger().warning("Unknown matcher '" + key + "'. Try one of these instead: " + Joiner.on(", ").join(closestMatches));
                } else {
                    SuperbVote.getPlugin().getLogger().warning("Unknown matcher '" + key + "'. Check your spelling.");
                }
            }
        }

        List<RewardMatcher> matchers = new ArrayList<>();
        for (RewardMatcherFactory factory : MATCHER_FACTORIES) {
            Optional<RewardMatcher> matcher;
            try {
                matcher = factory.create(section);
            } catch (IllegalArgumentException e) {
                SuperbVote.getPlugin().getLogger().severe("Invalid matcher found: " + e.getMessage());
                matcher = Optional.of(StaticRewardMatcher.ERROR);
            } catch (Exception e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to create matcher", e);
                matcher = Optional.of(StaticRewardMatcher.ERROR);
            }
            matcher.ifPresent(matchers::add);
        }

        // Clean up
        if (section.getBoolean("default")) {
            matchers.clear();
            matchers.add(StaticRewardMatcher.ALWAYS_MATCH);
        }

        return matchers;
    }

    static Optional<Permission> getVaultPermissions() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return Optional.empty();
        }
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            return Optional.of(rsp.getProvider());
        }
        return Optional.empty();
    }
}
