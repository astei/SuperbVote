package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
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
    private final List<PlayerVotes> top;

    private static final String UNKNOWN_USERNAME = "MHF_Question";
    public static final BlockFace[] FACES = {BlockFace.SELF, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

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
                for (int i = 0; i < 4; i++) {
                    worldSign.setLine(i, "???");
                }
            } else {
                for (int i = 0; i < Math.min(4, SuperbVote.getPlugin().getConfiguration().getTopPlayerSignsConfiguration().getSignText().size()); i++) {
                    PlainStringMessage m = SuperbVote.getPlugin().getConfiguration().getTopPlayerSignsConfiguration().getSignText().get(i);
                    worldSign.setLine(i, m.getWithOfflinePlayer(null, top.get(sign.getPosition() - 1)).replace("%num%",
                            Integer.toString(sign.getPosition())));
                }
                for (int i = SuperbVote.getPlugin().getConfiguration().getTopPlayerSignsConfiguration().getSignText().size(); i < 4; i++) {
                    worldSign.setLine(i, "");
                }
            }
            worldSign.update();

            // If a head location is also present, set the location for that.
            Optional<Block> headBlock = findSkullBlock(sign.getSign().getBukkitLocation().getBlock());
            if (headBlock.isPresent()) {
                Block head = headBlock.get();
                Skull skull = (Skull) head.getState();
                skull.setSkullType(SkullType.PLAYER);
                skull.setOwner(sign.getPosition() > top.size() ? UNKNOWN_USERNAME :
                        SuperbVote.getPlugin().getUuidCache().getNameFromUuid(top.get(sign.getPosition() - 1).getUuid()));
                skull.update();
            }
        }
    }
}
