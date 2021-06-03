package name.lmj0011.holdup.helpers.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.DateTimeHelper.getElapsedTimeUntilFutureTime
import name.lmj0011.holdup.helpers.DateTimeHelper.getLocalDateFromUtcMillis
import name.lmj0011.holdup.helpers.DateTimeHelper.getPostAtDateForListLayout
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.receivers.PublishScheduledSubmissionReceiver
import timber.log.Timber

/**
 * This Worker recreates the alarms for all Submissions scheduled in the future
 *
 */
class RefreshAlarmsWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    private val alarmMgr = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dao = AppDatabase.getInstance(appContext).sharedDao

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createForegroundInfo()
            delay(3000L) // give Foreground Notif time to show?
            val submissions = dao.getAllSubmissions()

            refreshAlarms(submissions)

            Result.success()
        } catch (ex:Exception) {
            Timber.e(ex)
            Result.failure()
        }
    }

    // https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running#foreground-service-type
    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHelper.getReschedulingSubmissionsForegroundServiceNotification()
        return ForegroundInfo(NotificationHelper.POSTING_SCHEDULED_SUBMISSION_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun refreshAlarms(submissions: List<Submission>) {
        submissions
            .forEach {
                val newAlarmIntent = Intent(appContext, PublishScheduledSubmissionReceiver::class.java).let { intent ->
                    intent.putExtra("alarmRequestCode", it.alarmRequestCode)
                    PendingIntent.getBroadcast(appContext, it.alarmRequestCode, intent, 0)
                }

                val futureElapsedTime = getElapsedTimeUntilFutureTime(it.postAtMillis)

                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    futureElapsedTime, // I have yet to understand how this coming up with the correct number
                    newAlarmIntent
                )
                Timber.d("alarm set for Submission: ${getPostAtDateForListLayout(it)}")

                dao.update(it)
            }
    }
}