package name.lmj0011.redditdraftking.database.models

import androidx.room.Embedded
import androidx.room.Relation

data class SubredditWithDrafts(
    @Embedded val subreddit: Subreddit,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "subreddit_uuid"
    )
    val drafts: List<Draft>
)