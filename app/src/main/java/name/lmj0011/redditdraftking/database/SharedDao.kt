package name.lmj0011.redditdraftking.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SharedDao: BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(draft: Draft)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(subreddit: Subreddit)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(account: Account)

    fun insert(draft: Draft) {
        actualInsert(setTimestamps(draft) as Draft)
    }
    fun insert(subreddit: Subreddit) {
        actualInsert(setTimestamps(subreddit) as Subreddit)
    }
    fun insert(account: Account) {
        actualInsert(setTimestamps(account) as Account)
    }

    // try to update, then insert row if it does not exists
    fun upsert(draft: Draft) {
        when(getDraft(draft.uuid)) {
            is Draft -> update(draft)
            else -> insert(draft)
        }
    }

    fun upsert(subreddit: Subreddit) {
        when(getSubreddit(subreddit.uuid)) {
            is Subreddit -> update(subreddit)
            else -> insert(subreddit)
        }
    }

    fun upsert(account: Account) {
        when(getAccountByName(account.name)) {
            is Account -> update(account)
            else -> insert(account)
        }
    }


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllDrafts(drafts: MutableList<Draft>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllSubreddits(subreddits: MutableList<Subreddit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllAccounts(accounts: MutableList<Account>)


    fun insertAllDrafts(drafts: MutableList<Draft>) {
        val list = drafts.map { setTimestamps(it) as Draft }.toMutableList()
        actualInsertAllDrafts(list)
    }
    fun insertAllSubreddits(subreddits: MutableList<Subreddit>) {
        val list = subreddits.map { setTimestamps(it) as Subreddit }.toMutableList()
        actualInsertAllSubreddits(list)
    }
    fun insertAllAccounts(accounts: MutableList<Account>) {
        val list = accounts.map { setTimestamps(it) as Account }.toMutableList()
        actualInsertAllAccounts(list)
    }

    @Update
    fun actualUpdate(draft: Draft)
    @Update
    fun actualUpdate(subreddit: Subreddit)
    @Update
    fun actualUpdate(account: Account)

    fun update(draft: Draft) {
        actualUpdate(setUpdatedAt(draft) as Draft)
    }
    fun update(subreddit: Subreddit) {
        actualUpdate(setUpdatedAt(subreddit) as Subreddit)
    }
    fun update(account: Account) {
        actualUpdate(setUpdatedAt(account) as Account)
    }

    @Update
    fun actualUpdateAllDrafts(drafts: MutableList<Draft>)
    @Update
    fun actualUpdateAllSubreddits(subreddits: MutableList<Subreddit>)
    @Update
    fun actualUpdateAllAccounts(accounts: MutableList<Account>)

    fun updateAllDrafts(drafts: MutableList<Draft>) {
        val list = drafts.map { setUpdatedAt(it) as Draft }.toMutableList()
        actualUpdateAllDrafts(list)
    }
    fun updateAllSubreddits(subreddits: MutableList<Subreddit>) {
        val list = subreddits.map { setUpdatedAt(it) as Subreddit }.toMutableList()
        actualUpdateAllSubreddits(list)
    }
    fun updateAllAccounts(accounts: MutableList<Account>) {
        val list = accounts.map { setUpdatedAt(it) as Account }.toMutableList()
        actualUpdateAllAccounts(list)
    }

    @Query("SELECT * from drafts_table WHERE uuid = :uuid")
    fun getDraft(uuid: String): Draft?
    @Query("SELECT * from subreddits_table WHERE uuid = :uuid")
    fun getSubreddit(uuid: String): Subreddit?
    @Query("SELECT * from accounts_table WHERE id = :id")
    fun getAccount(id: Long): Account?
    @Query("SELECT * from accounts_table WHERE name = :name")
    fun getAccountByName(name: String): Account?

    @Query("SELECT * FROM drafts_table")
    fun getAllDraftsObserverable(): LiveData<MutableList<Draft>>
    @Query("SELECT * FROM drafts_table")
    fun getAllDrafts(): MutableList<Draft>
    @Query("SELECT * FROM subreddits_table ORDER BY display_name COLLATE NOCASE ASC")
    fun getAllSubredditsObserverable(): LiveData<MutableList<Subreddit>>
    @Query("SELECT * FROM accounts_table ORDER BY name COLLATE NOCASE ASC")
    fun getAllAccounts(): MutableList<Account>

    @Query("DELETE from drafts_table WHERE uuid = :uuid")
    fun deleteByDraftId(uuid: String): Int
    @Query("DELETE from subreddits_table WHERE uuid = :uuid")
    fun deleteBySubredditId(uuid: String): Int
    @Query("DELETE from accounts_table WHERE id = :id")
    fun deleteByAccountId(id: Long): Int
    @Query("DELETE from accounts_table WHERE name = :name")
    fun deleteByAccountName(name: String): Int



    // Special queries
    @Query("SELECT * FROM drafts_table WHERE subreddit_uuid = :subredditUuid ORDER BY post_at_millis DESC, date_modified DESC")
    fun getAllDraftsBySubreddit(subredditUuid: String): MutableList<Draft>

    @Transaction
    @Query("SELECT * FROM subreddits_table ORDER BY display_name COLLATE NOCASE ASC")
    fun getSubredditWithDrafts(): LiveData<MutableList<SubredditWithDrafts>>
    /////////
}