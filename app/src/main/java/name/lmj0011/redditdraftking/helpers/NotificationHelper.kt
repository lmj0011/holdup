package name.lmj0011.redditdraftking.helpers

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import name.lmj0011.redditdraftking.MainActivity
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.Draft
import name.lmj0011.redditdraftking.database.Subreddit

object NotificationHelper {
    const val SCHEDULED_DRAFT_SERVICE_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#scheduledDraftService"
    const val SCHEDULED_DRAFT_SERVICE_NOTIFICATION_ID = 100
    const val DRAFT_PUBLISHED_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#draftPublished"
    const val BATTERY_OPTIMIZATION_INFO_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#batteryOptimizationInfo"
    const val BATTERY_OPTIMIZATION_INFO_NOTIFICATION_ID = 101
    lateinit var application: Application

    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        this.application = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel1 = NotificationChannel(DRAFT_PUBLISHED_CHANNEL_ID, "Draft Published", NotificationManager.IMPORTANCE_HIGH)

            val serviceChannel2 = NotificationChannel(SCHEDULED_DRAFT_SERVICE_CHANNEL_ID, "Scheduled Draft Service", NotificationManager.IMPORTANCE_MIN)
            serviceChannel2.setSound(null, null)

            val serviceChannel3 = NotificationChannel(BATTERY_OPTIMIZATION_INFO_CHANNEL_ID, "Battery Optimization info", NotificationManager.IMPORTANCE_HIGH)

            NotificationManagerCompat.from(application)
                .createNotificationChannels(mutableListOf(serviceChannel1, serviceChannel2, serviceChannel3))
        }
    }

    fun showDraftPublishedNotification(draftAndSub: Pair<Draft, Subreddit>, postUrl: String) {
        val draft = draftAndSub.first
        val subreddit = draftAndSub.second
        val viewUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(postUrl))
        val pendingIntent = PendingIntent.getActivity(application, 0, viewUrlIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = NotificationCompat.Builder(application, DRAFT_PUBLISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(true)
            .setContentTitle("Draft Published")
            .setContentText(draft.title)
            .setLargeIcon(Glide.with(application).asBitmap().load(subreddit.iconImgUrl).circleCrop().submit().get())
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .addAction(0, "View", pendingIntent)
            .build()

        NotificationManagerCompat.from(application)
            .notify(SystemClock.uptimeMillis().toInt(), notification)
    }

    fun showBatteryOptimizationInfoNotification() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        val settingsPendingIntent = PendingIntent.getActivity(application, 0, settingsIntent, 0)

        val moreInfoIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://developer.android.com/training/monitoring-device-state/doze-standby")
        }
        val moreInfoPendingIntent = PendingIntent.getActivity(application, 0, moreInfoIntent, 0)

        val notification =  NotificationCompat.Builder(application, BATTERY_OPTIMIZATION_INFO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(false)
            .setAutoCancel(true)
            .setContentTitle("Battery Optimization is enabled")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(application.getString(R.string.notification_battery_optimization_info_content_text))
            )
            .setContentIntent(settingsPendingIntent)
            .addAction(0, "More Info", moreInfoPendingIntent)
            .addAction(0, "Settings", settingsPendingIntent)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .build()

        NotificationManagerCompat.from(application)
            .notify(BATTERY_OPTIMIZATION_INFO_NOTIFICATION_ID, notification)
    }

    fun getScheduledDraftReminderForegroundServiceNotification(): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        return NotificationCompat.Builder(application, SCHEDULED_DRAFT_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(false)
            .setContentTitle("Scheduled Draft Service")
            .setContentText("Drafts posting soon.")
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .build()
    }

    fun getResetAlarmsForegroundServiceNotification(): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        return NotificationCompat.Builder(application, SCHEDULED_DRAFT_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(false)
            .setContentTitle("Rescheduling Drafts")
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .build()
    }
}