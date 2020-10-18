package name.lmj0011.redditdraftking.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
) : BaseEntity()