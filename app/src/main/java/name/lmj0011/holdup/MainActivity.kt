package name.lmj0011.holdup

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.ActivityMainBinding
import name.lmj0011.holdup.databinding.DialogAboutBinding
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.util.isIgnoringBatteryOptimizations
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.ui.accounts.AccountsViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration
    private val  viewModel by viewModels<AccountsViewModel> {
        ViewModelFactory(AppDatabase.getInstance(application).sharedDao, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.visibility = View.GONE
        setupNavigationListener()
        showFabAndSetListener(
            { navController.navigate(R.id.action_homeFragment_to_submissionFragment) },
            R.drawable.ic_baseline_edit_24
        )
    }

    override fun onPause() {
        super.onPause()

        if(!isIgnoringBatteryOptimizations(this)) {
            NotificationHelper.showBatteryOptimizationInfoNotification()
        }
    }

    private fun setupNavigationListener(){
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id){
                R.id.homeFragment -> {
                    launchIO {
                        val accountsSize = viewModel.getAccounts().size

                        withContext(Dispatchers.Main) {
                            showFabAndSetListener(
                                {
                                    if (accountsSize > 0) {
                                        navController.navigate(R.id.submissionFragment)
                                    } else {
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
                                },
                                R.drawable.ic_baseline_edit_24
                            )
                        }
                    }
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
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        if(BuildConfig.DEBUG) menuInflater.inflate(R.menu.main_debug, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_about -> {
                val aboutDialog = DialogAboutBinding.inflate(layoutInflater)
                if(BuildConfig.DEBUG) {
                    aboutDialog.versionTextView.text = "v${BuildConfig.VERSION_NAME} (${getString(R.string.app_build)}) DEBUG"
                } else {
                    aboutDialog.versionTextView.text = "v${BuildConfig.VERSION_NAME} (${getString(R.string.app_build)})"
                }

                MaterialAlertDialogBuilder(this@MainActivity).setView(aboutDialog.root).show()
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
            else -> super.onOptionsItemSelected(item)
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

    fun hideFab() {
        binding.fab.hide()
    }
}