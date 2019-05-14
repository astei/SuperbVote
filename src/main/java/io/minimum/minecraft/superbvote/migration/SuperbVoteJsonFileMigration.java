package io.minimum.minecraft.superbvote.migration;

import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.minimum.minecraft.superbvote.SuperbVote;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class SuperbVoteJsonFileMigration implements Migration {
    private final JsonParser parser = new JsonParser();

    @Override
    public String getName() {
        return "SuperbVote JSON storage";
    }

    @Override
    public void execute(ProgressListener listener) {
        File file = new File(SuperbVote.getPlugin().getDataFolder(), SuperbVote.getPlugin().getConfig().getString("storage.json.file"));
        JsonObject object;
        try (BufferedReader r = Files.newReader(file, StandardCharsets.UTF_8)) {
            object = parser.parse(r).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("Unable to open SuperbVote JSON file " + file, e);
        }

        // v1 format or v2?
        int records;
        int divisor;
        if (object.has("version")) {
            // v2.
            JsonObject recordObj = object.getAsJsonObject("records");
            records = recordObj.entrySet().size();
            divisor = ProgressUtil.findBestDivisor(records);
            listener.onStart(records);
            int currentIdx = 0;
            for (Map.Entry<String, JsonElement> entry : recordObj.entrySet()) {
                JsonObject o = entry.getValue().getAsJsonObject();

                int votes = o.getAsJsonPrimitive("votes").getAsInt();
                long lastVoted = o.getAsJsonPrimitive("lastVoted").getAsLong();
                SuperbVote.getPlugin().getVoteStorage().setVotes(UUID.fromString(entry.getKey()), votes, lastVoted);
                currentIdx++;

                if (currentIdx % divisor == 0) {
                    listener.onRecordBatch(currentIdx, records);
                }
            }
        } else {
            // v1.
            records = object.entrySet().size();
            divisor = ProgressUtil.findBestDivisor(records);
            listener.onStart(records);
            int currentIdx = 0;
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                int votes = entry.getValue().getAsInt();
                SuperbVote.getPlugin().getVoteStorage().setVotes(UUID.fromString(entry.getKey()), votes);
                currentIdx++;

                if (currentIdx % divisor == 0) {
                    listener.onRecordBatch(currentIdx, records);
                }
            }
        }

        listener.onFinish(records);
    }
}
