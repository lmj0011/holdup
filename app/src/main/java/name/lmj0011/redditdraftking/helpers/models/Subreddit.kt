package name.lmj0011.redditdraftking.helpers.models


data class Subreddit (
    val displayName: String,
    val displayNamePrefixed: String,
    val iconImgUrl: String,
    val subscribers: Int,
    val allowGalleries: Boolean,
    val allowImages: Boolean,
    val allowVideos: Boolean,
    val allowVideoGifs: Boolean,
    val allowPolls: Boolean,
    val linkFlairEnabled: Boolean
)