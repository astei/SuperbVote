package io.minimum.minecraft.superbvote.util;

import lombok.Value;

import java.util.UUID;

@Value
public class PlayerVotes {
    private final UUID uuid;
    private final int votes;
}
