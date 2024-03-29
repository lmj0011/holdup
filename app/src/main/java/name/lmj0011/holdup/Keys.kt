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
    val PUBLISH_SCHEDULED_SUBMISSION_WORKER_ID = stringPreferencesKey("pref_publish_scheduled_submission_worker_id")
    val LAST_SELECTED_DATE_TIME_FROM_CALENDAR = longPreferencesKey("pref_last_selected_date_time_from_calendar")

    // tabLayout positions for SubmissionFragment
    const val LINK_TAB_POSITION = 0
    const val IMAGE_TAB_POSITION = 1
    const val VIDEO_TAB_POSITION = 2
    const val SELF_TAB_POSITION = 3
    const val POLL_TAB_POSITION = 4

    // String Tags
    const val UPLOAD_SUBMISSION_MEDIA_WORKER_TAG = "name.lmj0011.holdup.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG"
    const val REFRESH_ACCOUNT_IMAGE_WORKER_TAG = "name.lmj0011.holdup.REFRESH_ACCOUNT_IMAGE_WORKER_TAG"
}