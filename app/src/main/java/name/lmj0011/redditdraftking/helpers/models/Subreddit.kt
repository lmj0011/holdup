package name.lmj0011.redditdraftking.helpers.models

import androidx.room.ColumnInfo

data class Subreddit (
    val displayName: String,
    val displayNamePrefixed: String,
    val iconImgUrl: String,
    val subscribers: Int
)