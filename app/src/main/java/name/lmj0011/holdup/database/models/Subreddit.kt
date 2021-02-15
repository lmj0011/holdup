package name.lmj0011.holdup.database.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import name.lmj0011.holdup.database.BaseEntity

@Keep
@Parcelize
@Entity(tableName = "subreddits_table")
data class Subreddit (
    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @PrimaryKey
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo(name = "display_name_prefixed")
    var displayNamePrefixed: String,

    @ColumnInfo(name = "icon_img_url")
    var iconImgUrl: String = "",

    @ColumnInfo(name = "url")
    var url: String,
) : BaseEntity(), Parcelable