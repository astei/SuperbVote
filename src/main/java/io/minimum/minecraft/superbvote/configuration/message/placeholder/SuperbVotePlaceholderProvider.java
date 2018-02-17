package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.votes.Vote;

public class SuperbVotePlaceholderProvider implements PlaceholderProvider {
    @Override
    public String apply(String message, MessageContext context) {
        String base = message.replace("%player%", context.getPlayer().getName())
                .replace("%votes%", Integer.toString(context.getVoteRecord().getVotes()))
                .replace("%uuid%", context.getPlayer().getUniqueId().toString());
        if (context.getVote().isPresent()) {
            Vote vote = context.getVote().get();
            base = base.replace("%service%", vote.getServiceName());
        }
        return base;
    }

    @Override
    public boolean canUse() {
        return true; // Only depends on SuperbVote components.
    }
}
