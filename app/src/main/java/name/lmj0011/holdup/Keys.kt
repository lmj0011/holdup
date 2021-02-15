package name.lmj0011.holdup

import androidx.datastore.preferences.core.preferencesKey

/**
 * global keys
 */
object Keys {
    const val UNIX_EPOCH_MILLIS = 0L
    const val FIFTEEN_MINUTES_IN_MILLIS = 900000L
    const val THIRTY_MINUTES_IN_MILLIS = 1800000L
    const val ONE_HOUR_IN_MILLIS = 3600000L

    // Worker tags
    const val SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG = "name.lmj0011.holdup.helpers.workers#ScheduledDraftServiceCallerWorker"

    // Datastore
    val SELECTED_ACCOUNT_USERNAME = preferencesKey<String>("pref_selected_account_username")
    val NEXT_RUNTIME_UNIQUE_INT = preferencesKey<Int>("pref_next_runtime_unique_int")
    val NEXT_RUNTIME_UNIQUE_LONG = preferencesKey<Long>("pref_next_runtime_unique_long")
    val ENABLE_INBOX_REPLIES = preferencesKey<Boolean>("pref_enable_inbox_replies")
}