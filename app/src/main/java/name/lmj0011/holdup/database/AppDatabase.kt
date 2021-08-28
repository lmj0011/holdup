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

@Database(entities = [Account::class, Submission::class], version = 2,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val sharedDao : SharedDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // alter Submissions table; add linkImageUrl column
                    database.execSQL("ALTER TABLE `submissions_table` ADD COLUMN `linkImageUrl` TEXT NOT NULL DEFAULT ''")
                } catch(ex: SQLiteException) {
                    val errMsg = ex.message?.trim()
                    if (errMsg != null && errMsg.contains("duplicate column name: linkImageUrl")) {
                        // ignore
                    } else throw ex
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
                        .build()
                }
                return instance
            }
        }
    }
}
