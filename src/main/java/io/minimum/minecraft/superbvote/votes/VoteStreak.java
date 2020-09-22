package io.minimum.minecraft.superbvote.votes;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class VoteStreak {
	private final UUID uuid;
	private final int count, days;
	private final List<String> lastDayServices;
}
