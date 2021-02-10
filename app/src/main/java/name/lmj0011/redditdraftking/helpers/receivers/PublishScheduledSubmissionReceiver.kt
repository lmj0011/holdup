package name.lmj0011.redditdraftking.helpers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import name.lmj0011.redditdraftking.helpers.workers.PublishScheduledSubmissionWorker
import timber.log.Timber
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class PublishScheduledSubmissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("PublishScheduledDraftReceiver.onReceive called!!")
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alarmRequestCode = intent.getIntExtra("alarmRequestCode", -1)
        Timber.d("alarmRequestCode: $alarmRequestCode")
        val data = Data.Builder()
            .putInt("alarmRequestCode", alarmRequestCode)
            .build()

        /**
         * Adding a random delay (some time within 1 min)
         * to prevent multiple PublishScheduledSubmissionWorkers running
         * at the same exact time and possibly spamming Reddit servers
         */
        val randGen = SecureRandom.getInstanceStrong()
        randGen.setSeed((Long.MIN_VALUE..Long.MAX_VALUE).random())
        val jitterTime = (randGen.nextInt(60) + 1).toLong() // 1-60 seconds
        Timber.d(" added jitter time is $jitterTime secs")
        /**
         *
         */

        val workRequest = OneTimeWorkRequestBuilder<PublishScheduledSubmissionWorker>()
            .setConstraints(constraints)
            .setInitialDelay(jitterTime, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueue(workRequest)
    }
}