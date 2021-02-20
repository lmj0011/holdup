package name.lmj0011.holdup.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.models.Submission

@Dao
interface SharedDao: BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(account: Account)
    @Insert
    fun actualInsert(submission: Submission)

    fun insert(account: Account) {
        actualInsert(setTimestamps(account) as Account)
    }
    fun insert(submission: Submission) {
        actualInsert(setTimestamps(submission) as Submission)
    }

    fun upsert(account: Account) {
        when(getAccountByName(account.name)) {
            is Account -> update(account)
            else -> insert(account)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllAccounts(accounts: MutableList<Account>)
    @Insert
    fun actualInsertAllSubmissions(submissions: MutableList<Submission>)

    fun insertAllAccounts(accounts: MutableList<Account>) {
        val list = accounts.map { setTimestamps(it) as Account }.toMutableList()
        actualInsertAllAccounts(list)
    }
    fun insertAllSubmissions(submissions: MutableList<Submission>) {
        val list = submissions.map { setTimestamps(it) as Submission }.toMutableList()
        actualInsertAllSubmissions(list)
    }

    @Update
    fun actualUpdate(account: Account)
    @Update
    fun actualUpdate(submission: Submission)

    fun update(account: Account) {
        actualUpdate(setUpdatedAt(account) as Account)
    }
    fun update(submission: Submission) {
        actualUpdate(setUpdatedAt(submission) as Submission)
    }

    @Update
    fun actualUpdateAllAccounts(accounts: MutableList<Account>)
    @Update
    fun actualUpdateAllSubmissions(submissions: MutableList<Submission>)

    fun updateAllAccounts(accounts: MutableList<Account>) {
        val list = accounts.map { setUpdatedAt(it) as Account }.toMutableList()
        actualUpdateAllAccounts(list)
    }
    fun updateAllSubmissions(submissions: MutableList<Submission>) {
        val list = submissions.map { setUpdatedAt(it) as Submission }.toMutableList()
        actualUpdateAllSubmissions(list)
    }

    @Query("SELECT * from accounts_table WHERE id = :id")
    fun getAccount(id: Long): Account?
    @Query("SELECT * from accounts_table WHERE name = :name")
    fun getAccountByName(name: String): Account?
    @Query("SELECT * from submissions_table WHERE id = :id")
    fun getSubmission(id: Long): Submission?
    @Query("SELECT * from submissions_table WHERE alarmRequestCode = :alarmRequestCode")
    fun getSubmissionByAlarmRequestCode(alarmRequestCode: Int): Submission?

    @Query("SELECT * FROM accounts_table ORDER BY name COLLATE NOCASE ASC")
    fun getAllAccounts(): MutableList<Account>
    @Query("SELECT * FROM submissions_table")
    fun getAllSubmissionsObserverable(): LiveData<MutableList<Submission>>
    @Query("SELECT * FROM submissions_table")
    fun getAllSubmissions(): MutableList<Submission>

    @Query("DELETE from accounts_table WHERE id = :id")
    fun deleteByAccountId(id: Long): Int
    @Query("DELETE from accounts_table WHERE name = :name")
    fun deleteByAccountName(name: String): Int
    @Query("DELETE from submissions_table WHERE id = :id")
    fun deleteBySubmissionId(id: Long): Int
    @Query("DELETE from submissions_table")
    fun deleteAllSubmissionId(): Int

    // Special Queries
    @Query("SELECT COUNT(*) FROM accounts_table")
    fun accountsRowCount(): Int
    @Query("SELECT COUNT(*) FROM submissions_table")
    fun submissionsRowCount(): Int

}