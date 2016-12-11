package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.votes.Vote;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class ScriptRewardMatcher implements RewardMatcher {
    private final ScriptEngine engine;
    private final Path path;

    public ScriptRewardMatcher(Path path) throws IOException, ScriptException {
        this.path = path;
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("JavaScript");
        try (Reader reader = Files.newBufferedReader(path)) {
            engine.eval(reader);
        }
    }

    @Override
    public boolean matches(Vote vote) {
        Invocable invocable = (Invocable) engine;
        try {
            Object result = invocable.invokeFunction("matchVote", vote);
            return result != null && (result.equals(Boolean.TRUE) || result.equals(0));
        } catch (ScriptException | NoSuchMethodException e) {
            SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Unable to execute 'matchVote' function in " + path, e);
            return false;
        }
    }
}
