package name.lmj0011.redditdraftking.helpers.enums

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
enum class DraftKind(val kind: String) : Parcelable {
    RichText("richtext"), // when submitting, pairs with SubmitKind.Self
    MarkDown("markdown"), // when submitting, pairs with SubmitKind.Self
    Link("link"); // when submitting, pairs with SubmitKind.Link

    // :prayer_hands: https://stackoverflow.com/a/34625163/2445763
    companion object {
        fun from(findKind: String): DraftKind = values().first { it.kind == findKind }
    }
}