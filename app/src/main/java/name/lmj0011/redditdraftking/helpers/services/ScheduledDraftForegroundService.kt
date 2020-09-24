package name.lmj0011.redditdraftking.helpers.services

import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import name.lmj0011.redditdraftking.helpers.NotificationHelper

/**
 * A Service with the sole purpose of keeping this App from being killed off before
 * Work enqueued by `PublishScheduledDraftReceiver` is dispatched.
 *
 * This Service will start some time before the next scheduled Draft is set to be published
 */
class ScheduledDraftForegroundService: LifecycleService() {

    companion object {
        lateinit var context: Context

        fun startService(context: Context) {
            this.context = context
            val startIntent = Intent(context, ScheduledDraftForegroundService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            this.context = context
            val stopIntent = Intent(context, ScheduledDraftForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val notification = NotificationHelper.getScheduledDraftReminderForegroundServiceNotification()
        startForeground(NotificationHelper.SCHEDULED_DRAFT_SERVICE_NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
        return null
    }
}