package io.minimum.minecraft.superbvote.migration;

public interface Migration {
    String getName();

    void execute(ProgressListener listener);
}
