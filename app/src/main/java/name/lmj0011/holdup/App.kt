package name.lmj0011.holdup

import android.app.Application
import androidx.work.*
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import name.lmj0011.holdup.helpers.*
import name.lmj0011.holdup.helpers.workers.RefreshAlarmsWorker
import name.lmj0011.holdup.helpers.workers.UploadSubmissionMediaWorker
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import timber.log.Timber
import java.util.concurrent.TimeUnit

class App: Application(), Configuration.Provider {
    @ExperimentalCoroutinesApi
    val kodein = DI.direct {
        bind<RedditApiHelper>() with singleton { RedditApiHelper(this@App) }
        bind<RedditAuthHelper>() with singleton { RedditAuthHelper(this@App) }
        bind<SubmissionValidatorHelper>() with singleton { SubmissionValidatorHelper(this@App) }
        bind<DataStoreHelper>() with singleton { DataStoreHelper(this@App) }
        bind<UniqueRuntimeNumberHelper>() with singleton { UniqueRuntimeNumberHelper(this@App) }
    }

    override fun onCreate() {
        super.onCreate()

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
        val workManager = WorkManager.getInstance(applicationContext)

        val uploadSubmissionMediaWorkRequest = PeriodicWorkRequestBuilder<UploadSubmissionMediaWorker>(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, // run every 15 minutes
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .addTag(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG, ExistingPeriodicWorkPolicy.REPLACE, uploadSubmissionMediaWorkRequest)
    }

    private fun enqueueOneTimeWorkers() {
        WorkManager.getInstance(applicationContext).enqueue(OneTimeWorkRequestBuilder<RefreshAlarmsWorker>().build())
    }
}