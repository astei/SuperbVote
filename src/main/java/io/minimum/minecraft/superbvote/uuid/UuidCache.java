package io.minimum.minecraft.superbvote.uuid;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// TODO: Maybe should use a supplementing local cache.
public class UuidCache {
    private final LoadingCache<String, UUID> nameCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(500)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(new CacheLoader<String, UUID>() {
                @Override
                public UUID load(@NonNull String name) throws Exception {
                    UUIDFetcher fetcher = new UUIDFetcher(ImmutableList.of(name));
                    Map<String, UUID> uuidMap = fetcher.call();
                    for (Map.Entry<String, UUID> entry : uuidMap.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
                    }
                    throw new Exception("Unable to find UUID for " + name);
                }

                @Override
                public ListenableFuture<UUID> reload(String name, UUID oldValue) throws Exception {
                    return ListenableFutureTask.create(() -> {
                        UUIDFetcher fetcher = new UUIDFetcher(ImmutableList.of(name));
                        Map<String, UUID> uuidMap = fetcher.call();
                        for (Map.Entry<String, UUID> entry : uuidMap.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
                        }
                        throw new Exception("Unable to find UUID for " + name);
                    });
                }
            });
    private final LoadingCache<UUID, String> uuidCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(500)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(new CacheLoader<UUID, String>() {
                @Override
                public String load(@NonNull UUID uuid) throws Exception {
                    List<String> names = NameFetcher.nameHistoryFromUuid(uuid);
                    if (names.isEmpty()) {
                        throw new Exception("Unable to resolve name for " + uuid);
                    }
                    return names.get(names.size() - 1);
                }
            });

    public void cachePlayer(Player player) {
        nameCache.put(player.getName().toLowerCase(), player.getUniqueId());
        uuidCache.put(player.getUniqueId(), player.getName());
    }

    public UUID getUuidFromName(String name) {
        Preconditions.checkNotNull(name, "name");
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return player.getUniqueId();
        try {
            return nameCache.get(name.toLowerCase());
        } catch (ExecutionException e) {
            return null;
        }
    }

    public String getNameFromUuid(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        try {
            return uuidCache.get(uuid);
        } catch (ExecutionException e) {
            return null;
        }
    }
}
