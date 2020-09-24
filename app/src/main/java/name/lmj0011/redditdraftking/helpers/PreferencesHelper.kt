package name.lmj0011.redditdraftking.helpers

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import name.lmj0011.redditdraftking.PreferenceKeys as Keys

class PreferencesHelper(val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun nextRuntimeUniqueInt() = prefs.getInt(Keys.nextRuntimeUniqueInt, 1)
    fun nextRuntimeUniqueInt(count: Int) = prefs.edit { putInt(Keys.nextRuntimeUniqueInt, count) }

    fun nextRuntimeUniqueLong() = prefs.getLong(Keys.nextRuntimeUniqueLong, 1L)
    fun nextRuntimeUniqueLong(count: Long) = prefs.edit { putLong(Keys.nextRuntimeUniqueLong, count) }
}