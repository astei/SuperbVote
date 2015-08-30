package io.minimum.minecraft.superbvote.uuid;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineModeUuidCache implements UuidCache {
    private final Map<UUID, String> uuidToName = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>(32, 0.75f, 2);
    private transient final Gson gson = new Gson();
    private transient final File saveTo;

    public OfflineModeUuidCache(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = new FileReader(file)) {
            OfflineModeUuidCache votes = gson.fromJson(reader, OfflineModeUuidCache.class);
            if (votes != null) {
                this.uuidToName.putAll(votes.uuidToName);
                this.nameToUuid.putAll(votes.nameToUuid);
            }
        }
    }

    @Override
    public void cachePlayer(Player player) {
        uuidToName.put(player.getUniqueId(), player.getName());
        nameToUuid.put(player.getName(), player.getUniqueId());
    }

    @Override
    public UUID getUuidFromName(String name) {
        if (nameToUuid.containsKey(name))
            return nameToUuid.get(name);
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        nameToUuid.put(name, uuid);
        uuidToName.put(uuid, name);
        return uuid;
    }

    @Override
    public String getNameFromUuid(UUID uuid) {
        return uuidToName.get(uuid);
    }

    public void save() {
        try (Writer writer = new FileWriter(saveTo)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save UUID cache to " + saveTo.getAbsolutePath(), e);
        }
    }
}
