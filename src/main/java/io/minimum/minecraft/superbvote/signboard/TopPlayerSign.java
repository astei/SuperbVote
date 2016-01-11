package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.util.SerializableLocation;
import lombok.Data;

@Data
public class TopPlayerSign {
    private final SerializableLocation sign;
    private final int position;
}
