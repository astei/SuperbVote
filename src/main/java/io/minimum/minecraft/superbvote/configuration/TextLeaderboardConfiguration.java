package io.minimum.minecraft.superbvote.configuration;

import io.minimum.minecraft.superbvote.configuration.message.OfflineVoteMessage;
import lombok.Data;

@Data
public class TextLeaderboardConfiguration {
    private final int perPage;
    private final OfflineVoteMessage header;
    private final OfflineVoteMessage entryText;
    private final OfflineVoteMessage pageNumberText;
}
