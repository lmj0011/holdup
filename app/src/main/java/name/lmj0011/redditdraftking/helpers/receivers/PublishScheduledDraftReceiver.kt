package name.lmj0011.redditdraftking.helpers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import name.lmj0011.redditdraftking.helpers.services.ResetAlarmsForegroundService
import name.lmj0011.redditdraftking.helpers.services.ScheduledDraftForegroundService
import name.lmj0011.redditdraftking.helpers.workers.PublishScheduledDraftWorker
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class PublishScheduledDraftReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentTimeInMillis = System.currentTimeMillis()

        Timber.d("cal.timeInMillis: ${cal.timeInMillis}")
        Timber.d("currentTimeInMillis: $currentTimeInMillis")

        Timber.d("PublishScheduledDraftReceiver.onReceive called!!")
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val draftUuid = intent.getStringExtra("draftUuid")
        val data = Data.Builder()
            .putString("draftUuid", draftUuid)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PublishScheduledDraftWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueue(workRequest)
    }
}