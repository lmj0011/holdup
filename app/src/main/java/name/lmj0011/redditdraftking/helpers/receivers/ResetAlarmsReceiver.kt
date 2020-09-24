package name.lmj0011.redditdraftking.helpers.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import name.lmj0011.redditdraftking.helpers.services.ResetAlarmsForegroundService
import name.lmj0011.redditdraftking.helpers.workers.ScheduledDraftServiceCallerWorker
import timber.log.Timber
import java.util.*

/**
 * A Receiver that calls a Worker to reset all alarms for this App
 */
class ResetAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ResetAlarmsForegroundService.startService(context)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ScheduledDraftServiceCallerWorker>().build())
    }
}