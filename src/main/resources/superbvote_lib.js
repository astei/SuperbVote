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