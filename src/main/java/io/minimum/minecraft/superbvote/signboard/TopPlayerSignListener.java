package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Optional;

public class TopPlayerSignListener implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (TopPlayerSign sign : SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()) {
            if (sign.getSign().getBukkitLocation().equals(event.getBlock().getLocation())) {
                // A sign is being destroyed.
                if (!event.getPlayer().hasPermission("superbvote.managesigns")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You can't destroy this sign.");
                    return;
                }

                // Otherwise, destroy this sign.
                SuperbVote.getPlugin().getTopPlayerSignStorage().removeSign(sign);
                event.getPlayer().sendMessage(ChatColor.RED + "Top voter sign unregistered.");
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), new TopPlayerSignFetcher(
                        SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()));
                return;
            } else {
                Optional<Block> headBlock = TopPlayerSignUpdater.findSkullBlock(event.getBlock());
                if (headBlock.isPresent()) {
                    event.setCancelled(true);
                    if (!event.getPlayer().hasPermission("superbvote.managesigns")) {
                        event.getPlayer().sendMessage(ChatColor.RED + "You can't destroy this skull.");
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "You can't destroy this skull. Destroy its sign first.");
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        try {
            if (event.getLine(0).startsWith("[topvoter]") && event.getPlayer().hasPermission("superbvote.managesigns")) {
                int p;
                try {
                    p = Integer.parseInt(event.getLine(1));
                } catch (NumberFormatException e) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "The second line needs to be a number.");
                    return;
                } catch (IndexOutOfBoundsException e) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "The second line does not exist.");
                    return;
                }

                TopPlayerSign sign = new TopPlayerSign(new SerializableLocation(event.getBlock().getWorld().getName(),
                        event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()), p);
                SuperbVote.getPlugin().getTopPlayerSignStorage().addSign(sign);
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), new TopPlayerSignFetcher(
                        SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()));

                event.getPlayer().sendMessage(ChatColor.GREEN + "Top voter sign registered.");
            }
        } catch (IndexOutOfBoundsException e) {
            // Ignore
        }
    }
}
