package name.lmj0011.redditdraftking.helpers.models

import org.json.JSONObject

/**
 * For moshi.adapter
 */
data class DraftsJsonResponse(val subreddits: List<SubredditJsonObject>, val drafts: List<DraftJsonObject>)

data class SubredditJsonObject(
    val name: String, // uid
    val icon_img: String,
    val community_icon: String,
    val display_name_prefixed: String,
    val display_name: String,
    val url: String
)

data class FlairJsonObject(
    val templateId: String, // uuid
    val text: String
)

data class DraftJsonObject(
    val body: Pair<JSONObject?, String?>?,
    val subreddit: String, // uid
    val flair: FlairJsonObject?,
    val id: String, // uuid
    val kind: String,
    val title: String,
    val created: Long,
    val modified: Long
)