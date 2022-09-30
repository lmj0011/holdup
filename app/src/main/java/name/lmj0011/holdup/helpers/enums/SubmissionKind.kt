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
        fun from(findKind: String): SubmissionKind {
            val first: SubmissionKind = values().first {
                val kind =
                    if (findKind.lowercase(Locale.getDefault()) == "text") Self.kind else findKind.lowercase(
                        Locale.getDefault()
                    )
                it.kind == kind
            }
            return first
        }
    }
}