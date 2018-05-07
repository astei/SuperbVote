// Defines a few functions to help create JS-based matchers.
var server = Java.type('org.bukkit.Bukkit').getServer();
var _SuperbVotePlugin = Java.type('io.minimum.minecraft.superbvote.SuperbVote');

// Assorted Bukkit helpers

/**
 * Runs the specified JavaScript function in the main server thread and returns its result.
 * @param f the function to call
 */
function callSync(f) {
    var pl = _SuperbVotePlugin.getPlugin();
    var future = server.scheduler.callSyncMethod(pl, f);
    return future.get();
}

// Assorted Java helpers

function _isTruthy(arg) {
    // Truthiness: a concept that JavaScript introduced well before Stephen Colbert told America about it
    // back in 2005.
    //
    // JavaScript drives me to drink. Colbert gives me a good laugh.
    // http://www.cc.com/video-clips/63ite2/the-colbert-report-the-word---truthiness
    return !!arg;
}