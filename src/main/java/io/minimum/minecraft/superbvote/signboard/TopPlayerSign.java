package io.minimum.minecraft.superbvote.signboard;

import lombok.Data;
import org.bukkit.Location;

import java.util.Optional;

@Data
public class TopPlayerSign {
    private final Location sign;
    private final Optional<Location> head;
    private final int position;
}
