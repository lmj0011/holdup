package name.lmj0011.holdup

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.MenuProvider
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.ActivityMainBinding
import name.lmj0011.holdup.databinding.DialogAboutBinding
import name.lmj0011.holdup.databinding.DialogFeedbackBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.services.PattonService
import name.lmj0011.holdup.helpers.util.*
import name.lmj0011.holdup.ui.accounts.AccountsViewModel
import org.kodein.di.instance
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dataStoreHelper: DataStoreHelper
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var mediaPlayer: SimpleExoPlayer

    private val  viewModel by viewModels<AccountsViewModel> {
        ViewModelFactory(AppDatabase.getInstance(application).sharedDao, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mediaPlayer = SimpleExoPlayer.Builder(this).build()

        dataStoreHelper = (applicationContext as App).kodein.instance()
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.visibility = View.GONE
        setupNavigationListener()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                this@MainActivity.createMenu(menu, menuInflater)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return this@MainActivity.menuItemSelected(menuItem)
            }

        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    override fun onResume() {
        mediaPlayer = SimpleExoPlayer.Builder(this).build()

        if(isIgnoringBatteryOptimizations(this)) {
            NotificationManagerCompat.from(this).cancel(NotificationHelper.BATTERY_OPTIMIZATION_INFO_NOTIFICATION_ID)
        }

        super.onResume()
    }

    private fun handleIntent(intent: Intent?) {
        when(intent?.action) {
            /**
             * Safe to assume PattonService is sending this Intent; no need to check if Service is
             * already running
             */
            PattonService.ACTION_NAV_TO_PATTON_SERVICE -> {
                navController.currentDestination?.let { navDestination ->
                    if((navDestination.id == R.id.pattonFragment).not())   {
                        navController.navigate(R.id.pattonFragment)
                    }
                }

            }
            else -> {
                Timber.e("No matching intent.action")
            }
        }
    }

    private fun setupNavigationListener(){
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            mediaPlayer.stop()
            when(destination.id){
                R.id.homeFragment -> {
                    launchIO {
                        val accountsSize = viewModel.getAccounts().size

                        withContext(Dispatchers.Main) {
                            showFabAndSetListener(
                                {
                                    if (accountsSize > 0) {
                                        navController.navigate(R.id.action_homeFragment_to_submissionFragment)
                                    } else {
                                        showRedditLoginMessage()
                                    }
                                },
                                R.drawable.ic_baseline_edit_24
                            )
                        }
                    }
                }
                R.id.submissionFragment, R.id.editSubmissionFragment -> {
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_action_clear)
                    hideFab()
                }
                R.id.accountsFragment -> {
                    CookieManager.getInstance().removeAllCookies{
                        if(it) Timber.d("webview Cookies were successfully cleared!")
                        else Timber.d("webview Cookies COULD NOT be cleared!")
                    }
                    showFabAndSetListener({ navController.navigate(R.id.redditAuthWebviewFragment) }, R.drawable.ic_baseline_add_24)
                }
                else -> hideFab()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        mediaPlayer.stop()
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun createMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the main menu
        inflater.inflate(R.menu.main, menu)

        // Add experimental menu items depending on this app build or versionName
        when {
            ((BuildConfig.DEBUG)
                    ||(BuildConfig.FLAVOR == "preview")
                    || BuildConfig.VERSION_NAME.contains("(alpha|beta)".toRegex())
                    ) -> {
                inflater.inflate(R.menu.main_preview, menu)
            }
        }
    }

    fun menuItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_leave_feedback -> {
                val feedbackDialog = DialogFeedbackBinding.inflate(layoutInflater)
                feedbackDialog.generalButton.setOnClickListener {
                    try {
                        sendGeneralFeedback(this@MainActivity)
                    } catch(ex: Exception) {
                        val msg = ex.message ?: "Could not open email app."
                        showSnackBar(binding.root, msg)
                    }
                }

                feedbackDialog.bugReportButton.setOnClickListener {
                    launchIO {
                        val prefs = dataStoreHelper.dataStore.data.first()
                        val dao = AppDatabase.getInstance(application).sharedDao
                        val template = getDebugDump(this@MainActivity, prefs, dao)

                        launchUI {
                            try {
                                sendBugReport(this@MainActivity, template)
                            } catch(ex: Exception) {
                                val msg = ex.message ?: "Could not open email app."
                                showSnackBar(binding.root, msg)
                            }
                        }
                    }
                }

                MaterialAlertDialogBuilder(this@MainActivity).setView(feedbackDialog.root).show()
                true
            }
            R.id.action_about -> {
                val aboutDialog = DialogAboutBinding.inflate(layoutInflater)
                aboutDialog.versionTextView.text = "v${BuildConfig.VERSION_NAME}"

                when {
                    (BuildConfig.DEBUG) -> {
                        aboutDialog.appNameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_baseline_bug_report_24, 0)
                    }
                }

                MaterialAlertDialogBuilder(this@MainActivity).setView(aboutDialog.root).show()
                true
            }
            R.id.action_patton -> {
                launchIO {
                    val accountsSize = viewModel.getAccounts().size

                    withContext(Dispatchers.Main) {
                        if (accountsSize > 0) {
                            navController.navigate(R.id.pattonFragment)
                        } else {
                            showRedditLoginMessage()
                        }
                    }
                }
                true
            }
            R.id.action_open_discord -> {
                launchUI {
                    openUrlInWebBrowser(this@MainActivity, resources.getString(R.string.holdup_discord_open_testing_channel_invite_url))
                }
                true
            }
            R.id.action_manage_accounts -> {
                navController.navigate(R.id.accountsFragment)
                true
            }
            R.id.action_testing -> {
                navController.navigate(R.id.testingFragment)
                true
            }
            else -> false
        }
    }

    fun showFabAndSetListener(cb: () -> Unit, imgSrcId: Int) {
        binding.fab.let {
            it.setOnClickListener(null) // should remove all attached listeners
            it.setOnClickListener { cb() }

            // hide and show to repaint the img src
            it.hide()
            it.setImageResource(imgSrcId)
            it.show()
        }
    }

    fun showRedditLoginMessage() {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setMessage("Log into your Reddit account to start scheduling Submissions")
            .setNeutralButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("") {_, _ -> }
            .setPositiveButton("Log In") { _, _ ->
                navController.navigate(R.id.redditAuthWebviewFragment)
            }
            .show()
    }

    fun hideFab() {
        binding.fab.hide()
    }
}