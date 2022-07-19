package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * ref: https://www.reddit.com/dev/api
 *
 * see fullnames section, this entity refers to t3_ which is a 	Link/Post
 */
@Keep
@Parcelize
data class Thing3 (
    val subreddit: String,
    val likes: Boolean,
    val authorFullname: String,
    val title: String,
    val subredditNamePrefixed: String,
    val downs: Int,
    val name: String,
    val upvoteRatio: Double,
    val ups: Int,
    val score: Int,
    val thumbnail: String,
    val isSelf: Boolean,
    val subredditType: String,
    val over18: Boolean,
    val subredditId: String,
    val author: String,
    val permalink: String
) : Parcelable