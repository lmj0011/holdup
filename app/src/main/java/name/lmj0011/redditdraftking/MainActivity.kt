package name.lmj0011.redditdraftking

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import name.lmj0011.redditdraftking.databinding.ActivityMainBinding
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.util.isIgnoringBatteryOptimizations
import name.lmj0011.redditdraftking.helpers.workers.ScheduledDraftServiceCallerWorker
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.homeFragment, R.id.navigation_dashboard, R.id.navigation_notifications))
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

        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<ScheduledDraftServiceCallerWorker>().build())
    }

    private fun setupNavigationListener(){
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id){
                R.id.homeFragment -> {
                    showFabAndSetListener(
                        { navController.navigate(R.id.action_homeFragment_to_submissionFragment) },
                        R.drawable.ic_baseline_edit_24
                    )
                }
                R.id.accountsFragment -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().removeAllCookies{
                            if(it) Timber.d("webview Cookies were successfully cleared!")
                            else Timber.d("webview Cookies COULD NOT be cleared!")
                        }
                    } else CookieManager.getInstance().removeAllCookie()
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
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