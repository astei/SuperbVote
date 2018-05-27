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

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public class RewardMatchers {
    private static final Set<String> AVAILABLE_MATCHERS = ImmutableSet.<String>builder()
            .add("permission")
            .add("chance-fractional")
            .add("chance-percentage")
            .add("chance")
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
        Set<String> keys = new HashSet<>(section.getKeys(false));
        keys.removeAll(AVAILABLE_MATCHERS);
        if (!keys.isEmpty()) {
            for (String key : keys) {
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

        // permission: <permission>
        String perm = section.getString("permission", null);
        if (perm != null) {
            if (!SuperbVote.getPlugin().getConfig().getBoolean("require-online")) {
                SuperbVote.getPlugin().getLogger().warning("'permission' vote rewards require that the player be online. Set 'require-online' to 'true' in your configuration.");
            }
            matchers.add(new PermissionRewardMatcher(perm));
        }

        // chance-fractional: <chance>
        Object chanceFracObject = section.get("chance-fractional");
        Object chanceObject = section.get("chance");
        if (chanceFracObject instanceof Integer) {
            if ((int) chanceFracObject < 1) {
                SuperbVote.getPlugin().getLogger().severe("Fraction " + chanceFracObject + " is not valid; must be 1 or more.");
                matchers.add(StaticRewardMatcher.NEVER_MATCH);
            } else {
                matchers.add(new ChanceFractionalRewardMatcher((int) chanceFracObject));
            }
        } else if (chanceObject instanceof Integer) {
            SuperbVote.getPlugin().getLogger().warning("The 'chance' vote matcher will be switched to be based on percentages out of 100% in a future release. Use 'chance-fractional' to " +
                    "retain the current behavior, or migrate to a percentage matcher by specifying 'chance-percentage' in your configuration.");
            if ((int) chanceObject < 1) {
                SuperbVote.getPlugin().getLogger().severe("Fraction " + chanceObject + " is not valid; must be 1 or more.");
                matchers.add(StaticRewardMatcher.NEVER_MATCH);
            } else {
                matchers.add(new ChanceFractionalRewardMatcher((int) chanceObject));
            }
        }

        // chance-percentage: <chance>
        Object chancePerObject = section.get("chance-percentage");
        if (chancePerObject instanceof Integer) {
            int chancePerInt = (int) chancePerObject;
            if (chancePerInt > 0 && chancePerInt < 100) {
                matchers.add(new ChancePercentageRewardMatcher(chancePerInt));
            } else {
                SuperbVote.getPlugin().getLogger().severe("Percentage " + chancePerInt + " is not valid; must be between 1 and 99.");
                matchers.add(StaticRewardMatcher.NEVER_MATCH);
            }
        }

        // service: <service> or services: <services>
        String service = section.getString("service", null);
        if (service != null) {
            matchers.add(new ServiceRewardMatcher(ImmutableList.of(service)));
        }
        List<String> services = section.getStringList("services");
        if (service == null && services != null && !services.isEmpty()) {
            matchers.add(new ServiceRewardMatcher(services));
        }

        // cumulative-votes: <votes>
        Object cumulativeObject = section.get("cumulative-votes");
        if (cumulativeObject instanceof Integer) {
            matchers.add(new CumulativeVotesRewardMatcher((int) cumulativeObject));
        }

        // every-cumulative-votes: <votes>
        Object everyCumulativeObject = section.get("every-cumulative-votes");
        if (everyCumulativeObject instanceof Integer) {
            matchers.add(new CumulativeVotesEveryRewardMatcher((int) everyCumulativeObject));
        }

        // script: <path>
        String script = section.getString("script");
        if (script != null) {
            try {
                matchers.add(new ScriptRewardMatcher(Paths.get(script)));
            } catch (IOException | ScriptException e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to parse script " + script, e);
                matchers.add(StaticRewardMatcher.NEVER_MATCH);
            }
        }

        // world: <world> or worlds: <worlds>
        String world = section.getString("world", null);
        if (world != null) {
            matchers.add(new WorldRewardMatcher(ImmutableList.of(world)));
        }
        List<String> worlds = section.getStringList("worlds");
        if (world == null && worlds != null && !worlds.isEmpty()) {
            matchers.add(new WorldRewardMatcher(worlds));
        }

        // groups: <groups> - Requires Vault
        Optional<Permission> vaultPermission = getVaultPermissions();
        String group = section.getString("group");
        List<String> groups = section.getStringList("groups");
        if (vaultPermission.isPresent()) {
            if (group != null && !group.isEmpty()) {
                matchers.add(new VaultGroupRewardMatcher(vaultPermission.get(), ImmutableList.of(group)));
            } else if (group == null && !groups.isEmpty()) {
                matchers.add(new VaultGroupRewardMatcher(vaultPermission.get(), groups));
            }
        } else if ((group != null && !group.isEmpty()) || !groups.isEmpty()) {
            SuperbVote.getPlugin().getLogger().warning("You can't use the 'group' or 'groups' matcher without having Vault installed.");
            matchers.add(StaticRewardMatcher.NEVER_MATCH);
        }

        // default: <true|false>
        if (section.getBoolean("default")) {
            if (!matchers.isEmpty()) {
                SuperbVote.getPlugin().getLogger().warning("Default matchers can not be used with any other matchers. SuperbVote will remove all other matchers.");
                matchers.clear();
            }
            matchers.add(StaticRewardMatcher.ALWAYS_MATCH);
        }

        return matchers;
    }

    private static Optional<Permission> getVaultPermissions() {
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
