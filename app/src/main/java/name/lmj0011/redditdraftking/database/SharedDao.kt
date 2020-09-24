package name.lmj0011.redditdraftking.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SharedDao: BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(draft: Draft)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsert(subreddit: Subreddit)

    fun insert(draft: Draft) {
        actualInsert(setTimestamps(draft) as Draft)
    }
    fun insert(subreddit: Subreddit) {
        actualInsert(setTimestamps(subreddit) as Subreddit)
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


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllDrafts(drafts: MutableList<Draft>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAllSubreddits(subreddits: MutableList<Subreddit>)


    fun insertAllDrafts(drafts: MutableList<Draft>) {
        val list = drafts.map { setTimestamps(it) as Draft }.toMutableList()
        actualInsertAllDrafts(list)
    }
    fun insertAllSubreddits(subreddits: MutableList<Subreddit>) {
        val list = subreddits.map { setTimestamps(it) as Subreddit }.toMutableList()
        actualInsertAllSubreddits(list)
    }

    @Update
    fun actualUpdate(draft: Draft)
    @Update
    fun actualUpdate(subreddit: Subreddit)

    fun update(draft: Draft) {
        actualUpdate(setUpdatedAt(draft) as Draft)
    }
    fun update(subreddit: Subreddit) {
        actualUpdate(setUpdatedAt(subreddit) as Subreddit)
    }

    @Update
    fun actualUpdateAllDrafts(drafts: MutableList<Draft>)
    @Update
    fun actualUpdateAllSubreddits(subreddits: MutableList<Subreddit>)

    fun updateAllDrafts(drafts: MutableList<Draft>) {
        val list = drafts.map { setUpdatedAt(it) as Draft }.toMutableList()
        actualUpdateAllDrafts(list)
    }
    fun updateAllSubreddits(subreddits: MutableList<Subreddit>) {
        val list = subreddits.map { setUpdatedAt(it) as Subreddit }.toMutableList()
        actualUpdateAllSubreddits(list)
    }

    @Query("SELECT * from drafts_table WHERE uuid = :uuid")
    fun getDraft(uuid: String): Draft?
    @Query("SELECT * from subreddits_table WHERE uuid = :uuid")
    fun getSubreddit(uuid: String): Subreddit?

    @Query("SELECT * FROM drafts_table")
    fun getAllDraftsObserverable(): LiveData<MutableList<Draft>>
    @Query("SELECT * FROM drafts_table")
    fun getAllDrafts(): MutableList<Draft>
    @Query("SELECT * FROM subreddits_table ORDER BY display_name COLLATE NOCASE ASC")
    fun getAllSubredditsObserverable(): LiveData<MutableList<Subreddit>>

    @Query("DELETE from drafts_table WHERE uuid = :uuid")
    fun deleteByDraftId(uuid: String): Int
    @Query("DELETE from subreddits_table WHERE uuid = :uuid")
    fun deleteBySubredditId(uuid: String): Int


    @Query("SELECT * FROM drafts_table WHERE subreddit_uuid = :subredditUuid ORDER BY post_at_millis ASC")
    fun getAllDraftsBySubreddit(subredditUuid: String): MutableList<Draft>

    @Transaction
    @Query("SELECT * FROM subreddits_table ORDER BY display_name COLLATE NOCASE ASC")
    fun getSubredditWithDrafts(): LiveData<MutableList<SubredditWithDrafts>>
}