package io.minimum.minecraft.superbvote.configuration.message;

import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.ClipsPlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.SuperbVotePlaceholderProvider;
import org.bukkit.entity.Player;

import java.util.List;

class MessageBase {
    private static final List<PlaceholderProvider> PROVIDER_LIST = ImmutableList.of(new SuperbVotePlaceholderProvider(),
            new ClipsPlaceholderProvider());

    String replace(String message, MessageContext context) {
        String replaced = message;
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse()) {
                replaced = provider.apply(replaced, context);
            }
        }
        return replaced;
    }
}
