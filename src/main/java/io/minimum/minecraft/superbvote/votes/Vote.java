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
    private final boolean fakeVote;
    private final String worldName;

    public Vote(String name, UUID uuid, String serviceName, boolean fakeVote, String worldName, Date received) {
        this.name = name;
        this.uuid = uuid;
        this.serviceName = serviceName;
        this.fakeVote = fakeVote;
        this.received = received;
        this.worldName = worldName;
    }
}
