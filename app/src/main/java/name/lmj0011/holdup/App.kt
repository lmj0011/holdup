package name.lmj0011.holdup

import android.app.Application
import androidx.work.*
import com.jakewharton.threetenabp.AndroidThreeTen
import name.lmj0011.holdup.helpers.*
import name.lmj0011.holdup.helpers.workers.RefreshAlarmsWorker
import org.kodein.di.*
import timber.log.Timber

class App: Application(), Configuration.Provider {
    lateinit var kodein: DirectDI

    override fun onCreate() {
        super.onCreate()
        kodein = DI.direct {
            bind<RedditApiHelper>() with singleton { RedditApiHelper(this@App) }
            bind<RedditAuthHelper>() with singleton { RedditAuthHelper(this@App) }
            bind<SubmissionValidatorHelper>() with singleton { SubmissionValidatorHelper(this@App) }
            bind<DataStoreHelper>() with singleton { DataStoreHelper(this@App) }
            bind<UniqueRuntimeNumberHelper>() with singleton { UniqueRuntimeNumberHelper(this@App) }
        }

        Timber.plant(Timber.DebugTree())
        AndroidThreeTen.init(this)
        NotificationHelper.init(this)
        enqueueOneTimeWorkers()
        enqueuePeriodicWorkers()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val builder = Configuration.Builder()

        return if (BuildConfig.DEBUG) builder.setMinimumLoggingLevel(android.util.Log.VERBOSE).build()
        else builder.build()
    }

    private fun enqueuePeriodicWorkers() {
//        val workManager = WorkManager.getInstance(applicationContext)
//        val requestCodeHelper: UniqueRuntimeNumberHelper  = (applicationContext as App).kodein.instance()
//
//        val scheduledDraftServiceCallerWorkRequest = PeriodicWorkRequestBuilder<ScheduledSubmissionServiceCallerWorker>(
//            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, // run every 15 minutes
//            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
//        )
//            .addTag(Keys.SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG)
//            .build()
//
//        workManager.enqueueUniquePeriodicWork(Keys.SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG, ExistingPeriodicWorkPolicy.REPLACE, scheduledDraftServiceCallerWorkRequest)
    }

    private fun enqueueOneTimeWorkers() {
        WorkManager.getInstance(applicationContext).enqueue(OneTimeWorkRequestBuilder<RefreshAlarmsWorker>().build())
    }
}