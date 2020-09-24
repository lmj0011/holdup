package name.lmj0011.redditdraftking.helpers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.Keys
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.DateTimeHelper
import name.lmj0011.redditdraftking.helpers.services.ScheduledDraftForegroundService
import name.lmj0011.redditdraftking.helpers.util.isIgnoringBatteryOptimizations
import timber.log.Timber
import java.util.*

/**
 * This Worker checks to see if there's any Drafts set to be posted within the next hour.
 * If there are any, `ScheduledDraftForegroundService` is started
 * otherwise `ScheduledDraftForegroundService` is stopped
 *
 */
class ScheduledDraftServiceCallerWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getInstance(appContext).sharedDao
            val drafts = dao.getAllDrafts()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            Timber.d("cal.timeInMillis: ${cal.timeInMillis}")

            val isUpcomingDraft = drafts.any {
                val localDate = DateTimeHelper.getLocalDateFromUtcMillis(it.postAtMillis)!!
                Timber.d("localDate.time > cal.timeInMillis")
                Timber.d("${localDate.time} > ${cal.timeInMillis}")
                // A Daft is set to post within the next 30 minutes
                localDate.time > cal.timeInMillis && (localDate.time - cal.timeInMillis) <= Keys.ONE_HOUR_IN_MILLIS
            }

            Timber.d("isUpcomingDraft: $isUpcomingDraft")

            if(isUpcomingDraft) {
                ScheduledDraftForegroundService.startService(appContext)
                Timber.d("Drafts posting soon found, starting `ScheduledDraftForegroundService`")
            } else {
                ScheduledDraftForegroundService.stopService(appContext)
                Timber.d("No Drafts posting soon, stopping `ScheduledDraftForegroundService`")
            }
        } catch (ex:Exception) {
            Timber.e(ex)
            ScheduledDraftForegroundService.stopService(appContext)
            Timber.d("Exception thrown, stopping `ScheduledDraftForegroundService`")
        }

        Result.success()
    }
}