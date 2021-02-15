package name.lmj0011.holdup.database.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.database.BaseEntity
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.models.SubredditFlair
import name.lmj0011.holdup.helpers.models.Video

@Keep
@Parcelize
@Entity(tableName = "submissions_table")
data class Submission (
    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "body")
    var body: String = "",

    @ColumnInfo(name = "url")
    var url: String = "", // for link submissions

    @ColumnInfo(name = "kind")
    var kind: SubmissionKind? = null,

    /**
     * - time in milliseconds since the UNIX epoch (Should be UTC time)
     * - if this equals [Keys.UNIX_EPOCH_MILLIS], then that means this submission
     * was posted "Now" by the user
     */
    @ColumnInfo(name = "postAtMillis")
    var postAtMillis: Long = Keys.UNIX_EPOCH_MILLIS,

    @ColumnInfo(name = "alarmRequestCode")
    var alarmRequestCode: Int = 0, // used as ID so we to cancel the right Alarm for this Draft

    @ColumnInfo(name = "subreddit")
    var subreddit: @RawValue Subreddit? = null, // the corresponding subreddit this submission belongs to

    @ColumnInfo(name = "account")
    var account: @RawValue Account? = null, // the Reddit account assoicated with this Submission

    @ColumnInfo(name = "subredditFlair")
    var subredditFlair: @RawValue SubredditFlair? = null,

    @ColumnInfo(name = "imgGallery")
    var imgGallery: @RawValue MutableList<Image> = mutableListOf(),

    @ColumnInfo(name = "video")
    var video: @RawValue Video? = null,

    @ColumnInfo(name = "pollOptions")
    var pollOptions: MutableList<String> = mutableListOf(),

    @ColumnInfo(name = "pollDuration")
    var pollDuration: Int = 3,

    @ColumnInfo(name = "isNsfw")
    var isNsfw: Boolean = false,

    @ColumnInfo(name = "isSpoiler")
    var isSpoiler: Boolean = false
) : BaseEntity(), Parcelable