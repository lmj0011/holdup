package name.lmj0011.redditdraftking

import android.app.Application
import androidx.work.*
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import com.jakewharton.threetenabp.AndroidThreeTen
import name.lmj0011.redditdraftking.helpers.*
import name.lmj0011.redditdraftking.helpers.services.ResetAlarmsForegroundService
import name.lmj0011.redditdraftking.helpers.workers.ScheduledDraftServiceCallerWorker
import org.kodein.di.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class App: Application(), Configuration.Provider {
    lateinit var kodein: DirectDI

    override fun onCreate() {
        super.onCreate()
        kodein = DI.direct {
            bind<RedditApiHelper>() with singleton { RedditApiHelper(this@App) }
            bind<RedditAuthHelper>() with singleton { RedditAuthHelper(this@App) }
            bind<PreferencesHelper>() with singleton { PreferencesHelper(this@App) }
            bind<UniqueRuntimeNumberHelper>() with singleton { UniqueRuntimeNumberHelper(this@App) }
        }

        Timber.plant(Timber.DebugTree())
        AndroidThreeTen.init(this)
        NotificationHelper.init(this)
        ResetAlarmsForegroundService.startService(this)
        enqueuePeriodicWorkers()


        SoLoader.init(this, false)
        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
            val client = AndroidFlipperClient.getInstance(this)
            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
            client.addPlugin(DatabasesFlipperPlugin(this))
            client.start()
        }

    }

    override fun getWorkManagerConfiguration(): Configuration {
        val builder = Configuration.Builder()

        return if (BuildConfig.DEBUG) builder.setMinimumLoggingLevel(android.util.Log.VERBOSE).build()
        else builder.build()
    }

    private fun enqueuePeriodicWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        val scheduledDraftServiceCallerWorkRequest = PeriodicWorkRequestBuilder<ScheduledDraftServiceCallerWorker>(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, // run every 15 minutes
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .addTag(Keys.SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(Keys.SCHEDULED_DRAFT_SERVICE_CALLER_WORKER_TAG, ExistingPeriodicWorkPolicy.REPLACE, scheduledDraftServiceCallerWorkRequest)
    }
}