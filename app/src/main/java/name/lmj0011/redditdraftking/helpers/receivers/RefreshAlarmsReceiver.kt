package name.lmj0011.redditdraftking.helpers.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import name.lmj0011.redditdraftking.helpers.workers.RefreshAlarmsWorker
import timber.log.Timber

/**
 * A Receiver that calls a Worker to reset all alarms for this App
 */
class RefreshAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("RefreshAlarmsReceiver called, dispatching work..")
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshAlarmsWorker>().build())
    }
}