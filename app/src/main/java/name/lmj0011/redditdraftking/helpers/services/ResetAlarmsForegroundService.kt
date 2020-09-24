package name.lmj0011.redditdraftking.helpers.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.UniqueRuntimeNumberHelper
import name.lmj0011.redditdraftking.helpers.DateTimeHelper
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.receivers.PublishScheduledDraftReceiver
import name.lmj0011.redditdraftking.helpers.util.launchIO
import org.kodein.di.instance
import timber.log.Timber
import java.util.*

class ResetAlarmsForegroundService: LifecycleService() {

    companion object {
        lateinit var context: Context

        fun startService(context: Context) {
            this.context = context
            val startIntent = Intent(context, ResetAlarmsForegroundService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            this.context = context
            val stopIntent = Intent(context, ResetAlarmsForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val notification = NotificationHelper.getResetAlarmsForegroundServiceNotification()
        startForeground(SystemClock.uptimeMillis().toInt(), notification)

        resetAlarms()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
        return null
    }

    private fun resetAlarms() {
        val dao = AppDatabase.getInstance(context).sharedDao
        val requestCodeHelper: UniqueRuntimeNumberHelper = (context.applicationContext as App).kodein.instance()
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        launchIO {
            try {
                val drafts = dao.getAllDrafts()
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                Timber.d("cal.timeInMillis: ${cal.timeInMillis}")

                drafts
                    .filter {
                        val localDate = DateTimeHelper.getLocalDateFromUtcMillis(it.postAtMillis)!!
                        localDate.time > cal.timeInMillis
                    }
                    .forEach {
                        val oldAlarmIntent = Intent(context, PublishScheduledDraftReceiver::class.java).let { intent ->
                            intent.putExtra("draftUuid", it.uuid)
                            PendingIntent.getBroadcast(context, it.requestCode, intent, 0)
                        }

                        it.requestCode = requestCodeHelper.nextInt()

                        val newAlarmIntent = Intent(context, PublishScheduledDraftReceiver::class.java).let { intent ->
                            intent.putExtra("draftUuid", it.uuid)
                            PendingIntent.getBroadcast(context, it.requestCode, intent, 0)
                        }

                        alarmMgr.cancel(oldAlarmIntent)

                        val localDate = DateTimeHelper.getLocalDateFromUtcMillis(it.postAtMillis)
                        if(localDate != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, localDate.time, newAlarmIntent)
                            } else {
                                alarmMgr.set(AlarmManager.RTC_WAKEUP, localDate.time, newAlarmIntent)
                            }
                            Timber.d("alarm set for Draft: $drafts")
                        } else {
                            // TODO - Show ERROR "Unable to `getLocalDateFromUtcMillis`"
                            // reset it.postAtMillis = Const.UNIX_EPOCH
                        }

                        dao.update(it)
                    }

//                // TODO? - notify scheduled draft posts that were missed (ie. currentTimeMillis >= postAtMillis)
//                drafts.filter {
//                    calendar.timeInMillis  >= it.postAtMillis
//                }.forEach {
//
//                }
                stopService(context)
            } catch (ex:Exception) {
                Timber.e(ex)
                stopService(context)
            }
        }
    }
}