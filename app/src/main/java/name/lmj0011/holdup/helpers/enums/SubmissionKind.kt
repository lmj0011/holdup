package name.lmj0011.holdup.helpers.enums

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
@Parcelize
enum class SubmissionKind(val kind: String) : Parcelable {
    Link("link"),
    Self("self"),
    Image("image"),
    Video("video"),
    Poll("poll"),
    VideoGif("videogif");

    // :prayer_hands: https://stackoverflow.com/a/34625163/2445763
    companion object {
        fun from(findKind: String): SubmissionKind = values().first { it.kind == findKind.lowercase(
            Locale.getDefault()
        ) }
    }
}