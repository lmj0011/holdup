package name.lmj0011.redditdraftking.database

import android.annotation.SuppressLint
import androidx.room.Dao
import java.time.ZoneOffset

@Dao
interface BaseDao {
    @SuppressLint("NewApi")
    fun setUpdatedAt(entity: BaseEntity): BaseEntity {
        val iso8061Date = java.time.ZonedDateTime.now(ZoneOffset.UTC).toString()
        entity.updatedAt = iso8061Date
        return entity
    }

    // sets both created_at and updated_at
    @SuppressLint("NewApi")
    fun setTimestamps(entity: BaseEntity): BaseEntity {
        val iso8061Date = java.time.ZonedDateTime.now(ZoneOffset.UTC).toString()
        entity.createdAt = iso8061Date
        entity.updatedAt = iso8061Date
        return entity
    }
}