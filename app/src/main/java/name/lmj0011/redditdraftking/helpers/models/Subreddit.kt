package name.lmj0011.redditdraftking.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Subreddit (
    val guid: String, // globally unique ID on reddit eg.) t5_15bfi0
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
) : Parcelable