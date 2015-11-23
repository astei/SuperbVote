package io.minimum.minecraft.superbvote.signboard;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;

public class TopPlayerSignListener implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (TopPlayerSign sign : SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()) {
            if (sign.getSign().equals(event.getBlock().getLocation())) {
                // A sign is being destroyed.
                if (!event.getPlayer().hasPermission("superbvote.managesigns")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You can't destroy this sign.");
                    return;
                }

                // Otherwise, destroy this sign.
                SuperbVote.getPlugin().getTopPlayerSignStorage().removeSign(sign);
                event.getPlayer().sendMessage(ChatColor.RED + "Top voter sign unregistered.");
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


}
