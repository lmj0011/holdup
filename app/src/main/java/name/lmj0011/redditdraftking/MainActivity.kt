package name.lmj0011.redditdraftking

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import org.kodein.di.instance
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditAuthHelperJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        redditAuthHelper = (applicationContext as App).kodein.instance()

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()
        redditAuthHelperJob = GlobalScope.launch(Dispatchers.Main) {
            redditAuthHelper.isAuthorized.collectLatest {
                Timber.d("redditAuthHelper.isAuthorized: $it")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        redditAuthHelperJob.cancel()
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
            R.id.action_auth_app -> {
                navController.navigate(R.id.redditAuthWebviewFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}