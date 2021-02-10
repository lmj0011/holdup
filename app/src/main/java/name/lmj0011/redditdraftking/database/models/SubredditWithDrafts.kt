package name.lmj0011.redditdraftking.database.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SubredditWithDrafts(
    @Embedded val subreddit: Subreddit,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "subreddit_uuid"
    )
    val drafts: List<Draft>
) : Parcelable