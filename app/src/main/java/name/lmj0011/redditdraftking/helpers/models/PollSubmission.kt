package name.lmj0011.redditdraftking.helpers.models

import android.net.Uri

data class PollSubmission (
    // common properties
    var title: String,
    var subreddit: String, // name of subreddit w/o the "r/" prefix; ex. "androiddev"
    var apiType: String = "json",
    var showErrorList: Boolean = true,
    var spoiler: Boolean = false,
    var nsfw: Boolean = false,
    var originalContent: Boolean = false,
    var postToTwitter: Boolean = false,
    var sendReplies: Boolean = true,
    var validateOnSubmit: Boolean = true,
    ////////

    var duration: Int = 3, // number of days
    var options: List<String>,
    var text: String = "" // Markdown
)