package io.minimum.minecraft.superbvote.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.storage.JsonVoteStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.handler.Vote;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SuperbVoteConfiguration {
    private final ConfigurationSection configuration;
    @Getter
    private final Map<String, VoteService> serviceMap = new HashMap<>();

    private static final List<String> SUPPORTED_STORAGE = ImmutableList.of("json");

    public SuperbVoteConfiguration(ConfigurationSection section) {
        this.configuration = section;

        ConfigurationSection serviceSection = section.getConfigurationSection("services");
        if (serviceSection == null) {
            throw new RuntimeException("No services defined.");
        }

        VoteService defaultService = deserializeService(serviceSection.getConfigurationSection("default"), null);
        serviceMap.put("default", defaultService);

        serviceSection.getKeys(false).stream()
                .filter(s -> !s.equals("default"))
                .map(serviceSection::getConfigurationSection)
                .map(s -> deserializeService(s, defaultService))
                .forEach(s -> serviceMap.put(s.getServiceName(), s));
    }

    private VoteService deserializeService(ConfigurationSection section, VoteService defaultService) {
        Preconditions.checkNotNull(section, "section is not valid; is the default service section missing?");

        String name = section.getName();
        boolean inheritDefault = section.getBoolean("inherit-default", true);

        List<String> commands = section.getStringList("commands");
        String broadcast = section.getString("broadcast-message", inheritDefault && defaultService != null ? defaultService.getBroadcastMessage() : null);
        String playerMessage = section.getString("player-message", inheritDefault && defaultService != null ? defaultService.getPlayerMessage() : null);

        if (broadcast == null) {
            throw new RuntimeException("'broadcast-message' missing in section '" + name + "' (do you need to enable 'inherit-default'?)");
        }

        if (playerMessage == null) {
            throw new RuntimeException("'player-message' missing in section '" + name + "' (do you need to enable 'inherit-default'?)");
        }

        if (inheritDefault && defaultService != null) {
            commands.addAll(defaultService.getCommands());
        }

        return new VoteService(name, commands, playerMessage, broadcast);
    }

    public VoteService getService(String serviceName) {
        VoteService service = SuperbVote.getPlugin().getConfiguration().getServiceMap().get(serviceName);
        if (service == null) {
            service = SuperbVote.getPlugin().getConfiguration().getServiceMap().get("default");
        }
        return service;
    }

    public boolean requirePlayersOnline() {
        return configuration.getBoolean("require-online", false);
    }

    public static String replacePlaceholders(String text, Vote vote) {
        return text.replaceAll("%player%", vote.getName()).replaceAll("%service%", vote.getService().getServiceName());
    }

    public VoteStorage initializeVoteStorage() throws IOException {
        String storage = configuration.getString("storage.database");
        if (!SUPPORTED_STORAGE.contains(storage)) {
            SuperbVote.getPlugin().getLogger().info("Storage method '" + storage + "' is not valid, using JSON storage.");
            storage = "json";
        }

        switch (storage) {
            case "json":
                String file = configuration.getString("storage.json.file");
                if (file == null) {
                    file = "votes.json";
                    SuperbVote.getPlugin().getLogger().info("No file found in configuration, using 'votes.json'.");
                }
                return new JsonVoteStorage(new File(SuperbVote.getPlugin().getDataFolder(), file));
        }

        return null;
    }
}
