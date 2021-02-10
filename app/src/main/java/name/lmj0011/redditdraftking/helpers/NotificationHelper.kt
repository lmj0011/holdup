package name.lmj0011.redditdraftking.helpers

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.MainActivity
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.database.models.Draft
import name.lmj0011.redditdraftking.database.models.Submission
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchNow
import org.kodein.di.instance
import timber.log.Timber
import java.net.URL

object NotificationHelper {
    const val SUBMISSION_PUBLISHED_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#submissionPublished"
    const val POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#scheduledSubmissionService"
    const val POSTING_SCHEDULED_SUBMISSION_SERVICE_NOTIFICATION_ID = 100
    const val BATTERY_OPTIMIZATION_INFO_CHANNEL_ID = "name.lmj0011.redditdraftking.helpers.NotificationHelper#batteryOptimizationInfo"
    const val BATTERY_OPTIMIZATION_INFO_NOTIFICATION_ID = 101
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private lateinit var application: Application

    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        this.application = application
        requestCodeHelper = (application as App).kodein.instance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val list = mutableListOf(
                NotificationChannel(BATTERY_OPTIMIZATION_INFO_CHANNEL_ID, "Battery Optimization info", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(SUBMISSION_PUBLISHED_CHANNEL_ID, "Submission Published", NotificationManager.IMPORTANCE_HIGH)
            )

            val sssServiceChannel = NotificationChannel(POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID, "Scheduled Submission Service", NotificationManager.IMPORTANCE_MIN)
            sssServiceChannel.setSound(null, null)
            list.add(sssServiceChannel)

            NotificationManagerCompat.from(application).createNotificationChannels(list)
        }
    }

    fun showSubmissionPublishedNotification(subreddit: Subreddit, account: Account, form: SubmissionValidatorHelper.SubmissionForm, postUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/user/${account.name.substring(2)}/?sort=new"))
        val defaultContentPendingIntent = PendingIntent.getActivity(application, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = NotificationCompat.Builder(application, SUBMISSION_PUBLISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentTitle("Submission successfully published")
            .setContentText(form.title)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .setContentIntent(defaultContentPendingIntent)

        postUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val customContentPendingIntent = PendingIntent.getActivity(application, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            notification.setContentIntent(customContentPendingIntent)
        }


        launchIO {
            try {
                val url = URL(subreddit.iconImgUrl)
                val image = BitmapFactory.decodeStream(url.openStream())
                notification
                    .setLargeIcon(image)
                    .setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .bigLargeIcon(null))
            } catch (ex: Exception) {
                Timber.e(ex)
            }

            NotificationManagerCompat.from(application)
                .notify(requestCodeHelper.nextInt(), notification.build())
        }
    }

    fun showSubmissionPublishedErrorNotification(submission: Submission, errorMessage: String) {
        val intent = Intent(application, MainActivity::class.java)
        val defaultContentPendingIntent = PendingIntent.getActivity(application, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = NotificationCompat.Builder(application, SUBMISSION_PUBLISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentTitle("Submission failed to publish to ${submission.subreddit?.displayNamePrefixed}")
            .setContentText(submission.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .setContentIntent(defaultContentPendingIntent)

        launchIO {
            NotificationManagerCompat.from(application)
                .notify(requestCodeHelper.nextInt(), notification.build())
        }
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

    fun getPostingSubmissionForegroundServiceNotification(submission: Submission): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        val builder = NotificationCompat.Builder(application, POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID)

        builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(false)
            .setContentTitle("Posting Scheduled Submission")
            .setContentText("${submission.title}")
            .setContentIntent(pendingIntent)
            .color = ContextCompat.getColor(application, R.color.colorPrimary)

        return builder.build()
    }

    fun getReschedulingSubmissionsForegroundServiceNotification(): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        return NotificationCompat.Builder(application, POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setShowWhen(false)
            .setContentTitle("Rescheduling Submissions")
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .build()
    }
}