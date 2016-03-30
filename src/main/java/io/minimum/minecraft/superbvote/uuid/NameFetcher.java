package io.minimum.minecraft.superbvote.uuid;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NameFetcher {
    private static final Gson gson = new Gson();

    public static List<String> nameHistoryFromUuid(UUID uuid) throws IOException {
        URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "").toLowerCase() + "/names").openConnection();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            Type listType = new TypeToken<List<Name>>() {}.getType();
            List<Name> names = gson.fromJson(reader, listType);

            if (names == null) {
                return ImmutableList.of();
            }

            return names.stream().map(name -> name.name).collect(Collectors.toList());
        }
    }

    public static class Name {
        private String name;
    }
}
