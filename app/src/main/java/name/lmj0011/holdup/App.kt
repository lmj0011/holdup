package name.lmj0011.holdup

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.work.*
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.*
import name.lmj0011.holdup.helpers.services.PattonService
import name.lmj0011.holdup.helpers.workers.RefreshAccountImageWorker
import name.lmj0011.holdup.helpers.workers.RefreshAlarmsWorker
import name.lmj0011.holdup.helpers.workers.UploadSubmissionMediaWorker
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.bind
import org.kodein.di.singleton
import timber.log.Timber
import java.util.concurrent.TimeUnit

class App: Application(), Configuration.Provider {
    lateinit var pattonService: PattonService
        private set

    private var _isPattonServiceBoundedFlow = MutableStateFlow(false)

    val isPattonServiceBoundedFlow: SharedFlow<Boolean>
        get() = _isPattonServiceBoundedFlow

    var isPattonServiceBounded: Boolean = false
        private set

    private val pattonConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to Service, cast the IBinder and get Service instance
            val binder = service as PattonService.PattonServiceBinder
            pattonService = binder.getService()
            isPattonServiceBounded = true
            _isPattonServiceBoundedFlow.tryEmit(isPattonServiceBounded)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isPattonServiceBounded = false
            _isPattonServiceBoundedFlow.tryEmit(isPattonServiceBounded)
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            isPattonServiceBounded = false
            _isPattonServiceBoundedFlow.tryEmit(isPattonServiceBounded)
        }
    }

    @ExperimentalCoroutinesApi
    val kodein: DirectDI = DI.direct {
        bind<RedditApiHelper>() with singleton { RedditApiHelper(this@App) }
        bind<RedditAuthHelper>() with singleton { RedditAuthHelper(this@App) }
        bind<SubmissionValidatorHelper>() with singleton { SubmissionValidatorHelper(this@App) }
        bind<DataStoreHelper>() with singleton { DataStoreHelper(this@App) }
        bind<UniqueRuntimeNumberHelper>() with singleton { UniqueRuntimeNumberHelper(this@App) }
        bind<PattonConnectivityHelper>() with singleton { PattonConnectivityHelper(this@App) }
    }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        AndroidThreeTen.init(this)
        NotificationHelper.init(this)
        enqueueOneTimeWorkers()
        enqueuePeriodicWorkers()

        /**
         * Bind to PattonService, we'll leave it to the OS to kill this Service
         * when it's no longer being used.
         *
         * start() still has to be called later to actually run Service logic
         */
        Intent(this, PattonService::class.java).also { intent ->
            bindService(intent, pattonConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startPattonService(acct: Account) {
        if (isPattonServiceBounded) {
            Timber.d("PattonService.start")
            pattonService.start(acct)
        }
    }

    fun stopPattonService() {
        if (isPattonServiceBounded) {
            Timber.d("PattonService.stop")
            pattonService.stop()
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val builder = Configuration.Builder()

        return if (BuildConfig.DEBUG) builder.setMinimumLoggingLevel(android.util.Log.VERBOSE).build()
        else builder.build()
    }

    private fun enqueuePeriodicWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        /**
         * checks Submissions for media that need to be uploaded; every 15 minutes
         */
        val uploadSubmissionMediaWorkRequest = PeriodicWorkRequestBuilder<UploadSubmissionMediaWorker>(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, // run every 15 minutes
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .addTag(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG, ExistingPeriodicWorkPolicy.REPLACE, uploadSubmissionMediaWorkRequest)

        /**
         * fetches Account icon images every 12 hours
         */
        val refreshAccountImageWorker = PeriodicWorkRequestBuilder<RefreshAccountImageWorker>(
            12, TimeUnit.HOURS, // run every 12 hours
            )
            .addTag(Keys.REFRESH_ACCOUNT_IMAGE_WORKER_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(Keys.REFRESH_ACCOUNT_IMAGE_WORKER_TAG, ExistingPeriodicWorkPolicy.REPLACE, refreshAccountImageWorker)
    }

    private fun enqueueOneTimeWorkers() {
        WorkManager.getInstance(applicationContext).enqueue(OneTimeWorkRequestBuilder<RefreshAlarmsWorker>().build())
    }
}