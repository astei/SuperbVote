package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;

public interface PlaceholderProvider {
    String apply(String message, MessageContext context);

    boolean canUse();
}
