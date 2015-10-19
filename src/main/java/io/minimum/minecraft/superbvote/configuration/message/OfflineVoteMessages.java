package io.minimum.minecraft.superbvote.configuration.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OfflineVoteMessages {
    public static OfflineVoteMessage from(ConfigurationSection root, String section) {
        if (root.isString(section)) {
            return new PlainStringMessage(root.getString(section));
        }
        throw new IllegalArgumentException("Section '" + section + "' (under " + root.getCurrentPath() + ") doesn't contain a valid message section.");
    }
}
