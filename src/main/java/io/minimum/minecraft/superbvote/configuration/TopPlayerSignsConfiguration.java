package io.minimum.minecraft.superbvote.configuration;

import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import lombok.Value;

import java.util.List;

@Value
public class TopPlayerSignsConfiguration {
    private final List<PlainStringMessage> signText;
}
