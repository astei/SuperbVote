package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class TopPlayerSignListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        for (TopPlayerSign sign : SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()) {
            if (sign.getSign().getBukkitLocation().equals(event.getBlock().getLocation())) {
                // A sign is being destroyed.
                if (!doSignBreak(event.getPlayer(), sign)) {
                    event.setCancelled(true);
                }
                return;
            }

            // A sign (which may be ours) may be supported on this block.
            for (BlockFace face : TopPlayerSignUpdater.FACES) {
                if (event.getBlock().getRelative(face).getLocation().equals(sign.getSign().getBukkitLocation())) {
                    if (!doSignBreak(event.getPlayer(), sign)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private boolean doSignBreak(Player player, TopPlayerSign sign) {
        // A sign is being destroyed.
        if (!player.hasPermission("superbvote.managesigns")) {
            player.sendMessage(ChatColor.RED + "You can't destroy this sign.");
            return false;
        }

        // Otherwise, destroy this sign.
        SuperbVote.getPlugin().getTopPlayerSignStorage().removeSign(sign);
        player.sendMessage(ChatColor.RED + "Top voter sign unregistered.");
        updateSigns();
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if ((event.getBlockPlaced().getType() == Material.SKELETON_WALL_SKULL ||
                event.getBlockPlaced().getType() == Material.SKELETON_SKULL) &&
                        event.getPlayer().hasPermission("superbvote.managesigns")) {
            Block down = event.getBlockPlaced().getRelative(BlockFace.DOWN);
            for (TopPlayerSign sign : SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()) {
                for (BlockFace face : TopPlayerSignUpdater.FACES) {
                    if (down.getRelative(face).getLocation().equals(sign.getSign().getBukkitLocation())) {
                        // We found an adjacent sign. Update so that the change will be reflected.
                        updateSigns();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        try {
            if (event.getLine(0).startsWith("[topvoter]") && event.getPlayer().hasPermission("superbvote.managesigns")) {
                int p;
                try {
                    p = Integer.parseInt(event.getLine(1).replaceAll("[^\\d]", ""));
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "The second line needs to be a number, indicating the position on the leaderboard this sign should display.");
                    event.getPlayer().sendMessage(ChatColor.RED + "For instance, to get the person with the most votes, use '1'.");
                    return;
                }

                TopPlayerSign sign = new TopPlayerSign(new SerializableLocation(event.getBlock().getWorld().getName(),
                        event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()), p);
                SuperbVote.getPlugin().getTopPlayerSignStorage().addSign(sign);
                updateSigns();

                event.getPlayer().sendMessage(ChatColor.GREEN + "Top voter sign registered.");
            }
        } catch (IndexOutOfBoundsException e) {
            // Ignore
        }
    }

    private void updateSigns() {
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), new TopPlayerSignFetcher(
                SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()));
    }
}
