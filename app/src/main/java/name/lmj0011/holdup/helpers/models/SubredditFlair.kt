package name.lmj0011.holdup.helpers.models

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