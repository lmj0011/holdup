package name.lmj0011.holdup.helpers

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spanned
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import name.lmj0011.holdup.App
import name.lmj0011.holdup.MainActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.util.launchIO
import org.kodein.di.instance
import timber.log.Timber
import java.net.URL

object NotificationHelper {
    const val SUBMISSION_PUBLISHED_CHANNEL_ID = "name.lmj0011.holdup.helpers.NotificationHelper#submissionPublished"
    const val POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID = "name.lmj0011.holdup.helpers.NotificationHelper#scheduledSubmissionService"
    const val UPLOADING_SUBMISSION_MEDIA_SERVICE_CHANNEL_ID = "name.lmj0011.holdup.helpers.NotificationHelper#uploadingSubmissionMediaService"
    const val BATTERY_OPTIMIZATION_INFO_CHANNEL_ID = "name.lmj0011.holdup.helpers.NotificationHelper#batteryOptimizationInfo"
    const val POSTING_SCHEDULED_SUBMISSION_SERVICE_NOTIFICATION_ID = 100
    const val BATTERY_OPTIMIZATION_INFO_NOTIFICATION_ID = 101
    const val UPLOADING_SUBMISSION_MEDIA_NOTIFICATION_ID = 102
    private lateinit var requestCodeHelper: UniqueRuntimeNumberHelper
    private lateinit var application: Application

    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        this.application = application
        requestCodeHelper = (application as App).kodein.instance()

        val chn1 = NotificationChannel(SUBMISSION_PUBLISHED_CHANNEL_ID, "Submission Published", NotificationManager.IMPORTANCE_HIGH)

        val chn2 = NotificationChannel(BATTERY_OPTIMIZATION_INFO_CHANNEL_ID, "Battery Optimization info", NotificationManager.IMPORTANCE_DEFAULT)
        chn2.setSound(null, null)

        val chn3 = NotificationChannel(POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID, "Scheduled Submission Service", NotificationManager.IMPORTANCE_MIN)
        chn3.setSound(null, null)

        val chn4 = NotificationChannel(UPLOADING_SUBMISSION_MEDIA_SERVICE_CHANNEL_ID, "Upload Submission Media Service", NotificationManager.IMPORTANCE_MIN)
        chn4.setSound(null, null)

        val list = mutableListOf(chn1, chn2, chn3, chn4)
        NotificationManagerCompat.from(application).createNotificationChannels(list)
    }

    fun showSubmissionPublishedNotification(subreddit: Subreddit, account: Account, form: SubmissionValidatorHelper.SubmissionForm, postUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/user/${account.name.substring(2)}/?sort=new"))
        val defaultContentPendingIntent = PendingIntent.getActivity(application, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = NotificationCompat.Builder(application, SUBMISSION_PUBLISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notification_icon)
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

    fun showSubmissionPublishedErrorNotification(submission: Submission, errorMessage: Spanned) {

        val pendingIntent = NavDeepLinkBuilder(application)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.editSubmissionFragment)
            .setArguments(Bundle().apply {
                putParcelable("submission", submission)
            })
            .createPendingIntent()

        val notification = NotificationCompat.Builder(application, SUBMISSION_PUBLISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notification_icon)
            .setAutoCancel(true)
            .setContentTitle("Your scheduled ${submission.kind?.name} Submission failed to publish.")
            .setContentText(submission.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .setContentIntent(pendingIntent)

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
            .setSmallIcon(R.drawable.ic_app_notification_icon)
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
            .setSmallIcon(R.drawable.ic_app_notification_icon)
            .setShowWhen(false)
            .setContentTitle("Posting Scheduled Submission")
            .setContentText("${submission.title}")
            .setContentIntent(pendingIntent)
            .color = ContextCompat.getColor(application, R.color.colorPrimary)

        return builder.build()
    }

    fun getUploadingSubmissionMediaForegroundServiceNotification(progress: Int = 0): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        val builder = NotificationCompat.Builder(application, UPLOADING_SUBMISSION_MEDIA_SERVICE_CHANNEL_ID)

        builder
            .setSmallIcon(R.drawable.ic_app_notification_icon)
            .setShowWhen(false)
            .setContentTitle("Uploading Submission Media")
            .setContentIntent(pendingIntent)
            .color = ContextCompat.getColor(application, R.color.colorPrimary)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    fun getReschedulingSubmissionsForegroundServiceNotification(): Notification {
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)

        return NotificationCompat.Builder(application, POSTING_SCHEDULED_SUBMISSION_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notification_icon)
            .setShowWhen(false)
            .setContentTitle("Rescheduling Submissions")
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(application, R.color.colorPrimary))
            .build()
    }
}