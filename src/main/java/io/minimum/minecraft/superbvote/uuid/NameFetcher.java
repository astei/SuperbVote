package io.minimum.minecraft.superbvote.uuid;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NameFetcher {
    private static final Gson gson = new Gson();

    public static List<String> nameHistoryFromUuid(UUID uuid) throws IOException {
        URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "").toLowerCase() + "/names").openConnection();

        String text;

        try (InputStream is = connection.getInputStream()) {
            text = new String(ByteStreams.toByteArray(is), Charsets.UTF_8);
        }

        if (text.isEmpty()) return Collections.emptyList();

        Type listType = new TypeToken<List<Name>>() {}.getType();
        List<Name> names = gson.fromJson(text, listType);

        return names.stream().map(name -> name.name).collect(Collectors.toList());
    }

    public static class Name {
        private String name;
    }
}
