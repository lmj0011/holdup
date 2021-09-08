package name.lmj0011.holdup.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.R

@ExperimentalCoroutinesApi
class DataStoreHelper(val context: Context) {
    val dataStore: DataStore<Preferences> = context.createDataStore(
        name = "preferences"
    )

    fun getSelectedAccountUsername(): Flow<String?> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.SELECTED_ACCOUNT_USERNAME]
        }
    }
    suspend fun setSelectedAccountUsername(username: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_ACCOUNT_USERNAME] = username
        }
    }

    fun getNextRuntimeUniqueInt(): Flow<Int> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_INT] ?: UniqueRuntimeNumberHelper.INITIAL_NEXT_INT
        }
    }
    suspend fun setNextRuntimeUniqueInt(count: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_INT] = count
        }
    }

    fun getNextRuntimeUniqueLong(): Flow<Long> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_LONG] ?: UniqueRuntimeNumberHelper.INITIAL_NEXT_LONG
        }
    }
    suspend fun setNextRuntimeUniqueLong(count: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_LONG] = count
        }
    }

    fun getEnableInboxReplies(): Flow<Boolean> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.ENABLE_INBOX_REPLIES] ?: true
        }
    }
    suspend fun setEnableInboxReplies(enable: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ENABLE_INBOX_REPLIES] = enable
        }
    }

    fun getIsMediaPlayerMuted(): Flow<Boolean> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.IS_MEDIA_PLAYER_MUTED] ?: false
        }
    }
    suspend fun setIsMediaPlayerMuted(enable: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_MEDIA_PLAYER_MUTED] = enable
        }
    }


    /**
     * Submission filter options
     */
    fun getSubmissionsDisplayOption(): Flow<String> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.SUBMISSIONS_DISPLAY_OPTION] ?: context.resources.getString(R.string.submissions_display_option_full_list)
        }
    }
    suspend fun setSubmissionsDisplayOption(option: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SUBMISSIONS_DISPLAY_OPTION] = option
        }
    }
    /**
     *
     */


    fun getPublishScheduledSubmissionWorkerId(): Flow<String> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.PUBLISH_SCHEDULED_SUBMISSION_WORKER_ID] ?: ""
        }
    }
    suspend fun setPublishScheduledSubmissionWorkerId(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PUBLISH_SCHEDULED_SUBMISSION_WORKER_ID] = id
        }
    }
}