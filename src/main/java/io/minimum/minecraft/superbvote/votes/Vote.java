package io.minimum.minecraft.superbvote.votes;

import lombok.Data;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Data
public class Vote {
    private final String name;
    private final UUID uuid;
    private final String serviceName;
    private final Date received;
    private final boolean fakeVote;
    private final long randomSeed;

    public Vote(String name, UUID uuid, String serviceName, boolean fakeVote, Date received) {
        this.name = name;
        this.uuid = uuid;
        this.serviceName = serviceName;
        this.fakeVote = fakeVote;
        this.received = received;
        this.randomSeed = new Random().nextLong();
    }

    /**
     * Returns a random generator seeded with a vote-specific seed. This is used to ensure reward chances are
     * deterministic per vote.
     * @return a {@link Random} instance
     */
    public Random getDeterministicGenerator() {
        return new Random(randomSeed);
    }
}
