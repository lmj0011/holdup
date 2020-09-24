package name.lmj0011.redditdraftking.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import name.lmj0011.redditdraftking.Keys

@Entity(tableName = "drafts_table")
data class Draft(
    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @PrimaryKey
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "subreddit_uuid")
    var subredditUuid: String = "",

    @ColumnInfo(name = "kind")
    var kind: String,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "body")
    var body: String = "",

    @ColumnInfo(name = "flair_id")
    var flairId: String? = null,

    @ColumnInfo(name = "flair_text")
    var flairText: String? = null,

    @ColumnInfo(name = "post_at_millis")
    var postAtMillis: Long = Keys.UNIX_EPOCH_MILLIS, // time in milliseconds since the UNIX epoch (Should be UTC time)

    @ColumnInfo(name = "request_code")
    var requestCode: Int = 0, // needed to in order to cancel the right Alarm for this Draft

) : BaseEntity()