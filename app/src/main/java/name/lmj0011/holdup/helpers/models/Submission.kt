package name.lmj0011.holdup.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import name.lmj0011.holdup.Keys

@Keep
@Parcelize
data class Submission (
    var title: String = "",
    var body: String = "",
    var url: String = "", // for link submissions
    var kind: String = "",

    /**
     * - time in milliseconds since the UNIX epoch (Should be UTC time)
     * - if this equals [Keys.UNIX_EPOCH_MILLIS], then that means this submission
     * was posted "Now" by the user
     */
    var postAtMillis: Long = Keys.UNIX_EPOCH_MILLIS,

    var alarmRequestCode: Int = 0, // used as ID so we to cancel the right Alarm for this Draft
    var subreddit: Subreddit? = null, // the corresponding subreddit this submission belongs to
    var subredditFlair: SubredditFlair? = null,
    var imgGallery: MutableList<Image> = mutableListOf(),
    var video: Video? = null,

    var pollOptions: MutableList<String> = mutableListOf(),
    var pollDuration: Int = 3,

    var isNsfw: Boolean = false,
    var isSpoiler: Boolean = false
) : Parcelable