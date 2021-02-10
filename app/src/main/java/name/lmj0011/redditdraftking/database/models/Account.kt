package name.lmj0011.redditdraftking.database.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import name.lmj0011.redditdraftking.database.BaseEntity

@Keep
@Parcelize
@Entity(tableName = "accounts_table")
data class Account (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "icon_image")
    var iconImage: String = "",
) : BaseEntity(), Parcelable