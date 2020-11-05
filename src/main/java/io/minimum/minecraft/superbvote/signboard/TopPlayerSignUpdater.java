package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class TopPlayerSignUpdater implements Runnable {
    private final List<TopPlayerSign> toUpdate;
    private final List<PlayerVotes> top;

    private static final UUID QUESTION_MARK_HEAD = UUID.fromString("606e2ff0-ed77-4842-9d6c-e1d3321c7838");
    static final BlockFace[] FACES = {BlockFace.SELF, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    public static Optional<Block> findSkullBlock(Block origin) {
        Block at = origin.getRelative(BlockFace.UP);
        for (BlockFace face : FACES) {
            Block b = at.getRelative(face);
            if (b.getType() == Material.PLAYER_HEAD || b.getType() == Material.PLAYER_WALL_HEAD)
                return Optional.of(b);
        }
        return Optional.empty();
    }

    @Override
    public void run() {
        for (TopPlayerSign sign : toUpdate) {
            Location location = sign.getSign().getBukkitLocation();
            if (location.getWorld() == null) {
                SuperbVote.getPlugin().getLogger().severe("World for sign " + sign.getSign() + " is missing!");
                continue;
            }
            Block block = location.getBlock();
            if (block.getState() instanceof Sign) {
                Sign worldSign = (Sign) block.getState();
                // TODO: Formatting
                if (sign.getPosition() > top.size()) {
                    for (int i = 0; i < 4; i++) {
                        worldSign.setLine(i, "???");
                    }
                } else {
                    int lines = SuperbVote.getPlugin().getConfiguration().getTopPlayerSignsConfiguration().getSignText().size();
                    for (int i = 0; i < Math.min(4, lines); i++) {
                        PlainStringMessage m = SuperbVote.getPlugin().getConfiguration().getTopPlayerSignsConfiguration().getSignText().get(i);
                        PlayerVotes pv = top.get(sign.getPosition() - 1);
                        worldSign.setLine(i, m.getWithOfflinePlayer(null,
                                new MessageContext(null, pv, null, null)).replace("%num%", Integer.toString(sign.getPosition())));
                    }
                    for (int i = lines; i < 4; i++) {
                        worldSign.setLine(i, "");
                    }
                }
                worldSign.update();

                // If a head location is also present, set the location for that.
                Optional<Block> headBlock = findSkullBlock(sign.getSign().getBukkitLocation().getBlock());
                if (headBlock.isPresent()) {
                    Block head = headBlock.get();
                    Skull skull = (Skull) head.getState();
                    skull.setOwningPlayer(Bukkit.getOfflinePlayer(sign.getPosition() > top.size() ? QUESTION_MARK_HEAD :
                            top.get(sign.getPosition() - 1).getUuid()));
                    skull.update();
                }
            }
        }
    }
}
