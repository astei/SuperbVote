package io.minimum.minecraft.superbvote.handler;

import io.minimum.minecraft.superbvote.configuration.VoteService;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class Vote {
    private final String name;
    private final UUID uuid;
    private final VoteService service;
    private final String serviceName;
    private final Date received;
}
