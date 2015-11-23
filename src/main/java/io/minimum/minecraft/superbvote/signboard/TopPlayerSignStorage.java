package io.minimum.minecraft.superbvote.signboard;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class TopPlayerSignStorage {
    private final List<TopPlayerSign> signList = new ArrayList<>();

    public List<TopPlayerSign> getSignList() {
        return ImmutableList.copyOf(signList);
    }

    public void addSign(TopPlayerSign sign) {
        signList.add(sign);
    }

    public void removeSign(TopPlayerSign sign) {
        signList.remove(sign);
    }
}
