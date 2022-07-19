package name.lmj0011.holdup

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.ActivityMainBinding
import name.lmj0011.holdup.databinding.ActivityPattonBinding
import name.lmj0011.holdup.databinding.DialogAboutBinding
import name.lmj0011.holdup.databinding.DialogFeedbackBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.services.PattonService
import name.lmj0011.holdup.helpers.util.*
import name.lmj0011.holdup.ui.accounts.AccountsViewModel
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class PattonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPattonBinding
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var isPattonServiceBoundedJob: Job
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_patton)

        dataStoreHelper = (applicationContext as App).kodein.instance()
        navController = findNavController(R.id.nav_host_fragment)

        /**
         * Doing this reveals the up button on all Fragments in the nav graph
         * ref: https://stackoverflow.com/a/60174566/2445763
         */
        appBarConfiguration = AppBarConfiguration(setOf())
        binding.navView.visibility = View.GONE

        isPattonServiceBoundedJob = launchIO {
            /**
             * Application gets an instance of Service AFTER it has been bounded, so
             * we need to wait until then before initializing PattonFragament
             */
            (applicationContext as App).isPattonServiceBoundedFlow.collect { bounded ->
                if(bounded) {
                    withUIContext {
                        setSupportActionBar(binding.toolbar)
                        setupActionBarWithNavController(navController, appBarConfiguration)
                        binding.navView.setupWithNavController(navController)
                        handleIntent(intent)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPattonServiceBoundedJob.cancel()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        /**
         * Since PattonFragment is the only screen for this Activity, we'll end this Activity on nav up
         */
        finish()
        return true
    }


    private fun handleIntent(intent: Intent?) {
        /**
         * Making sure we're bounded and have a connection to a Patton server before handling Intents
         */
        val isPattonServiceAvailable = (applicationContext as App).isPattonServiceBounded && PattonService.hasActiveSocketConnection

        when(intent?.action) {
            Intent.ACTION_SEND -> {
                intent.extras?.apply {
                    if(isPattonServiceAvailable) {
                        val url = getCharSequence(Intent.EXTRA_TEXT)
                        val payload = JSONObject().put("url", url)
                        (applicationContext as App).pattonService.emitUpvoteSubmission(payload)
                        moveTaskToBack(true)
                    } else {
                        showSnackBar(binding.root, "Turn on Patton Service to enable submitting Reddit posts")
                    }
                }
            }
            else -> {
                Timber.e("No matching intent.action")
            }
        }
    }
}