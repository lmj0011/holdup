package name.lmj0011.holdup.database

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.models.Submission

@Database(entities = [Account::class, Submission::class], version = 3,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val sharedDao : SharedDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // alter Submissions table; add linkImageUrl column
                    database.execSQL("ALTER TABLE submissions_table ADD COLUMN linkImageUrl TEXT NOT NULL DEFAULT ''")
                } catch(ex: SQLiteException) {
                    val errMsg = ex.message?.trim()
                    if (errMsg != null && errMsg.contains("duplicate column name: linkImageUrl")) {
                        // ignore
                    } else throw ex
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2,3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("SELECT * FROM `submissions_table`")

                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val colIdIdx = cursor.getColumnIndex("id")
                    val id = cursor.getInt(colIdIdx)

                    val colSubredditIdx = cursor.getColumnIndex("subreddit")
                    val rawJson = cursor.getString(colSubredditIdx)

                    val updatedRawJson =  submissionSubredditV2ToV3(rawJson)

                    database.execSQL("""UPDATE submissions_table SET subreddit ='${updatedRawJson}' WHERE ID = $id""")
                    cursor.moveToNext()
                }
            }
        }


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
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                }
                return instance
            }
        }

        private fun submissionSubredditV2ToV3(rawJson: String): String {
            val rawJsonOpenEnded = rawJson.dropLast(1)
            val newProperty = "\"over18\":false"

            return "$rawJsonOpenEnded,$newProperty}"
        }
    }
}
