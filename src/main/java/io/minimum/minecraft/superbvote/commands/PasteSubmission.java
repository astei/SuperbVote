package io.minimum.minecraft.superbvote.commands;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PasteSubmission {
    public static String submitPaste(String text) throws IOException {
        String toPost = "poster=SuperbVote&syntax=text&content=" + URLEncoder.encode(text, "UTF-8");

        URL url = new URL("http://paste.ubuntu.com");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);

        try (Writer writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(toPost);
        }

        String loc = connection.getHeaderField("Location");
        if (loc == null) {
            throw new IOException("Could not paste text.");
        }

        return loc;
    }
}
