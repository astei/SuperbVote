package io.minimum.minecraft.superbvote.signboard;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TopPlayerSignStorage {
    private final List<TopPlayerSign> signList = new ArrayList<>();
    private final Gson gson = new Gson();

    public List<TopPlayerSign> getSignList() {
        return ImmutableList.copyOf(signList);
    }

    public void addSign(TopPlayerSign sign) {
        signList.add(sign);
    }

    public void removeSign(TopPlayerSign sign) {
        signList.remove(sign);
    }

    public void load(File file) throws IOException {
        if (file.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                signList.addAll(gson.fromJson(reader, new TypeToken<List<TopPlayerSign>>() {
                }.getType()));
            }
        }
    }

    public void save(File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(signList, writer);
        }
    }
}
