package name.lmj0011.holdup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * global keys
 */
object Keys {
    const val UNIX_EPOCH_MILLIS = 0L

    // Datastore
    val SELECTED_ACCOUNT_USERNAME = stringPreferencesKey("pref_selected_account_username")
    val NEXT_RUNTIME_UNIQUE_INT = intPreferencesKey("pref_next_runtime_unique_int")
    val NEXT_RUNTIME_UNIQUE_LONG = longPreferencesKey("pref_next_runtime_unique_long")
    val ENABLE_INBOX_REPLIES = booleanPreferencesKey("pref_enable_inbox_replies")
    val IS_MEDIA_PLAYER_MUTED = booleanPreferencesKey("pref_is_media_player_muted")
    val SUBMISSIONS_DISPLAY_OPTION = stringPreferencesKey("pref_submissions_display_option")
}