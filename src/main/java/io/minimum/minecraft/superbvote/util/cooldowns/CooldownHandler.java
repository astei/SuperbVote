package io.minimum.minecraft.superbvote.util.cooldowns;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.superbvote.SuperbVote;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CooldownHandler<T> {
    private final ConcurrentMap<T, LocalDateTime> cooldowns = new ConcurrentHashMap<>(32, 0.75f, 1);
    @Getter
    private final int max;

    public CooldownHandler(int max) {
        this.max = max;
    }

    public boolean triggerCooldown(T obj) {
        Preconditions.checkNotNull(obj, "obj");

        LocalDateTime lastTime = cooldowns.get(obj);
        LocalDateTime now = LocalDateTime.now();
        if (lastTime == null || lastTime.isBefore(now)) {
            cooldowns.put(obj, now.plusSeconds(max));
            return false;
        }
        return true;
    }
}
