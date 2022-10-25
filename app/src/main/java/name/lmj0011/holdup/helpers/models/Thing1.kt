package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * ref: https://www.reddit.com/dev/api
 *
 * see fullnames section, this entity refers to t1_ which is a Comment
 */
@Keep
@Parcelize
data class Thing1 (
    val subreddit: String,
    val likes: Boolean,
    val authorFullname: String,
    val subredditNamePrefixed: String,
    val body: String,
    val downs: Int,
    val name: String,
    val ups: Int,
    val score: Int,
    val subredditType: String,
    val subredditId: String,
    val author: String,
    val permalink: String
) : Parcelable