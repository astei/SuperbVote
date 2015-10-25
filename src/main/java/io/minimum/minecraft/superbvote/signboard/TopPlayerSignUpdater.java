package io.minimum.minecraft.superbvote.signboard;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;

import java.util.List;

@RequiredArgsConstructor
public class TopPlayerSignUpdater implements Runnable {
    private final List<TopPlayerSign> toUpdate;
    private final List<String> top;

    private static final String UNKNOWN_USERNAME = "MHF_Question";

    @Override
    public void run() {
        for (TopPlayerSign sign : toUpdate) {
            Block block = sign.getSign().getBlock();
            switch (block.getType()) {
                case SIGN_POST:
                case WALL_SIGN:
                    break;
                default:
                    // Not a sign, bail out.
                    continue;
            }

            Sign worldSign = (Sign) block.getState();
            // TODO: Formatting
            if (sign.getPosition() > top.size()) {
                worldSign.setLine(0, "???");
            } else {
                worldSign.setLine(0, top.get(sign.getPosition() - 1));
            }
            worldSign.update();

            // If a head location is also present, set the location for that.
            if (sign.getHead().isPresent()) {
                Block headBlock = sign.getHead().get().getBlock();
                if (headBlock.getType() != Material.SKULL_ITEM) {
                    continue;
                }

                Skull skull = (Skull) headBlock.getState();
                skull.setSkullType(SkullType.PLAYER);
                skull.setOwner(sign.getPosition() > top.size() ? UNKNOWN_USERNAME : top.get(sign.getPosition() - 1));
                skull.update();
            }
        }
    }
}
