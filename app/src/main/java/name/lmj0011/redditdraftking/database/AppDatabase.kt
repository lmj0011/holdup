package name.lmj0011.redditdraftking.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.database.models.Draft
import name.lmj0011.redditdraftking.database.models.Submission
import name.lmj0011.redditdraftking.database.models.Subreddit

@Database(entities = [Draft::class, Subreddit::class, Account::class, Submission::class], version = 1,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val sharedDao : SharedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                        .build()
                }
                return instance
            }
        }
    }
}
