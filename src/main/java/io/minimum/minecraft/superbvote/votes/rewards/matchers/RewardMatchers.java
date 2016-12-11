package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.configuration.ConfigurationSection;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class RewardMatchers {
    public static List<RewardMatcher> getMatchers(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();
        List<RewardMatcher> matchers = new ArrayList<>();

        // permission: <permission>
        String perm = section.getString("permission", null);
        if (perm != null) {
            if (!SuperbVote.getPlugin().getConfig().getBoolean("require-online")) {
                SuperbVote.getPlugin().getLogger().warning("'permission' vote rewards require that the player be online. Set 'require-online' to 'true' in your configuration.");
            }
            matchers.add(new PermissionRewardMatcher(perm));
        }

        // chance: <chance>
        Object chanceObject = section.get("chance");
        if (chanceObject != null && chanceObject instanceof Integer) {
            matchers.add(new ChanceRewardMatcher((int) chanceObject));
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
        if (cumulativeObject != null && cumulativeObject instanceof Integer) {
            matchers.add(new CumulativeVotesRewardMatcher((int) cumulativeObject));
        }

        // script: <path>
        String script = section.getString("script");
        if (script != null) {
            try {
                matchers.add(new ScriptRewardMatcher(Paths.get(script)));
            } catch (IOException | ScriptException e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to parse script " + script, e);
            }
        }

        return matchers;
    }
}
