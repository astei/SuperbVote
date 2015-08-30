package io.minimum.minecraft.superbvote.votes;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class Vote {
    private final String name;
    private final UUID uuid;
    private final String serviceName;
    private final Date received;
}
