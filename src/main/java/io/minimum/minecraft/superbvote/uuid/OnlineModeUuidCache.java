package io.minimum.minecraft.superbvote.uuid;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// TODO: Maybe should use a supplementing local cache.
public class OnlineModeUuidCache implements UuidCache {
    private final LoadingCache<String, UUID> nameCache = Caffeine.newBuilder()
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(500)
            .build(k -> {
                UUIDFetcher fetcher = new UUIDFetcher(ImmutableList.of(k));
                Map<String, UUID> uuidMap = fetcher.call();
                for (Map.Entry<String, UUID> entry : uuidMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(k)) return entry.getValue();
                }
                throw new Exception("Unable to find UUID for " + k);
            });
    private final LoadingCache<UUID, String> uuidCache = Caffeine.newBuilder()
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(500)
            .build(k -> {
                List<String> names = NameFetcher.nameHistoryFromUuid(k);
                if (names.isEmpty()) {
                    throw new Exception("Unable to resolve name for " + k);
                }
                return names.get(names.size() - 1);
            });

    private final Gson gson = new Gson();
    private final File saveTo;

    public OnlineModeUuidCache(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new FileReader(file)) {
            Map<String, UUID> cache = gson.fromJson(reader, new TypeToken<Map<String, UUIDFetcher>>() {}.getType());
            if (cache != null) {
                for (Map.Entry<String, UUID> entry : cache.entrySet()) {
                    nameCache.put(entry.getKey(), entry.getValue());
                    uuidCache.put(entry.getValue(), entry.getKey());
                }
            }
        }
    }

    @Override
    public void cachePlayer(Player player) {
        nameCache.put(player.getName().toLowerCase(), player.getUniqueId());
        uuidCache.put(player.getUniqueId(), player.getName());
    }

    @Override
    public UUID getUuidFromName(String name) {
        Preconditions.checkNotNull(name, "name");
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return player.getUniqueId();

        return nameCache.get(name.toLowerCase());
    }

    @Override
    public String getNameFromUuid(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();

        return uuidCache.get(uuid);
    }

    public void save() {
        try (Writer writer = new FileWriter(saveTo)) {
            gson.toJson(nameCache.asMap(), writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save UUID cache to " + saveTo.getAbsolutePath(), e);
        }
    }
}
