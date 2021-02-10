package name.lmj0011.redditdraftking.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import name.lmj0011.redditdraftking.Keys

class DataStoreHelper(val context: Context) {
    private val dataStore: DataStore<Preferences> = context.createDataStore(
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

    fun getNextRuntimeUniqueInt(): Flow<Int?> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_INT]
        }
    }
    suspend fun setNextRuntimeUniqueInt(count: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_INT] = count
        }
    }

    fun getNextRuntimeUniqueLong(): Flow<Long?> {
        return dataStore.data.mapLatest { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_LONG]
        }
    }
    suspend fun setNextRuntimeUniqueLong(count: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_RUNTIME_UNIQUE_LONG] = count
        }
    }
}