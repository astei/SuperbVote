function matchVote(vote) {
    return palindrome(vote.getServiceName());
}

// This is a very rough way to check if a word is a palindrome.
function palindrome(word) {
    var cleaned = word.toLowerCase().replace(/[^a-z0-9]/g, "");
    return cleaned.split("").reverse().join("") == cleaned;
}