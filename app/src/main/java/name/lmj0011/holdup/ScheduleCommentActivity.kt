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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.ActivityMainBinding
import name.lmj0011.holdup.databinding.ActivityPattonBinding
import name.lmj0011.holdup.databinding.ActivityScheduleCommentBinding
import name.lmj0011.holdup.databinding.DialogAboutBinding
import name.lmj0011.holdup.databinding.DialogFeedbackBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.ExtractRedditThingFromUrl
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.services.PattonService
import name.lmj0011.holdup.helpers.util.*
import name.lmj0011.holdup.ui.accounts.AccountsViewModel
import name.lmj0011.holdup.ui.submission.ScheduleCommentViewModel
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class ScheduleCommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleCommentBinding
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var viewModel: ScheduleCommentViewModel
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ScheduleCommentViewModel.getInstance(
            AppDatabase.getInstance(application).sharedDao,
            application
        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_schedule_comment)

        dataStoreHelper = (applicationContext as App).kodein.instance()
        navController = findNavController(R.id.nav_host_fragment)

        /**
         * Doing this reveals the up button on all Fragments in the nav graph
         * ref: https://stackoverflow.com/a/60174566/2445763
         */
        appBarConfiguration = AppBarConfiguration(setOf())
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        launchIO {
            viewModel.setAccount()
            withUIContext { handleIntent(intent) }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        launchIO {
            viewModel.setAccount()
            withUIContext { handleIntent(intent) }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        /**
         * Since PattonFragment is the only screen for this Activity, we'll end this Activity on nav up
         */
        finish()
        return true
    }


    private fun handleIntent(intent: Intent?) {
        when(intent?.action) {
            Intent.ACTION_SEND -> {
                intent.extras?.apply {
                    val url = getCharSequence(Intent.EXTRA_TEXT)

                    when(val thing = ExtractRedditThingFromUrl.extract(url.toString())) {
                        is ExtractRedditThingFromUrl.Post -> {
                            launchIO { viewModel.getPostById(thing.fullName) }
                        }
                        is ExtractRedditThingFromUrl.Comment -> {
                            launchIO { viewModel.getCommentById(thing.fullName, thing.subredditNamePrefixed) }
                            launchIO { viewModel.getPostById(thing.fullNameOfParent) }
                        }
                    }
                }
            }
            else -> {
                Timber.e("No matching intent.action")
            }
        }
    }
}