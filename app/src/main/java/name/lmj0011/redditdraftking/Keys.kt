package name.lmj0011.redditdraftking

/**
 * global keys
 */
object Keys {
    const val UNIX_EPOCH_MILLIS = 0L
    const val FIFTEEN_MINUTES_IN_MILLIS = 900000L
    const val THIRTY_MINUTES_IN_MILLIS = 1800000L
    const val ONE_HOUR_IN_MILLIS = 3600000L

    // Worker tags
    const val SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG = "name.lmj0011.redditdraftking.helpers.workers#ScheduledDraftServiceCallerWorker"
}

/**
 * global keys specific to Preferences
 */
object PreferenceKeys {
    const val nextRuntimeUniqueInt = "pref_next_runtime_unique_int"
    const val nextRuntimeUniqueLong = "pref_next_runtime_unique_long"
}