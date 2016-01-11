package io.minimum.minecraft.superbvote.signboard;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TopPlayerSignUpdater implements Runnable {
    private final List<TopPlayerSign> toUpdate;
    private final List<String> top;

    private static final String UNKNOWN_USERNAME = "MHF_Question";
    private static final BlockFace[] FACES = {BlockFace.SELF, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    public static Optional<Block> findSkullBlock(Block origin) {
        Block at = origin.getRelative(BlockFace.UP);
        for (BlockFace face : FACES) {
            Block b = at.getRelative(face);
            if (b.getType() == Material.SKULL)
                return Optional.of(b);
        }
        return Optional.empty();
    }

    @Override
    public void run() {
        for (TopPlayerSign sign : toUpdate) {
            Block block = sign.getSign().getBukkitLocation().getBlock();
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
            Optional<Block> headBlock = findSkullBlock(sign.getSign().getBukkitLocation().getBlock());
            if (headBlock.isPresent()) {
                Block head = headBlock.get();
                Skull skull = (Skull) head.getState();
                skull.setSkullType(SkullType.PLAYER);
                skull.setOwner(sign.getPosition() > top.size() ? UNKNOWN_USERNAME : top.get(sign.getPosition() - 1));
                skull.update();
            }
        }
    }
}
