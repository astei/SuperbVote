package io.minimum.minecraft.superbvote.configuration;

import lombok.Data;

@Data
public class StreaksConfiguration {
    private final boolean enabled, placeholdersEnabled, sharedCooldownPerService;
}
