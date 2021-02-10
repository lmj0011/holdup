package name.lmj0011.redditdraftking.helpers.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SubredditFlair (
    val id: String,
    val text: String,
    val textColor: String,
    val backGroundColor: String
) : Parcelable