package name.lmj0011.holdup.helpers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import org.kodein.di.instance
import timber.log.Timber

/**
 * This Worker refreshes the profile pic of all Accounts
 *
 */
class RefreshAccountImageWorker (appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    private val dao = AppDatabase.getInstance(appContext).sharedDao
    private var redditAuthHelper: RedditAuthHelper = (appContext as App).kodein.instance()
    private val redditApiHelper: RedditApiHelper = (appContext as App).kodein.instance()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val accounts = dao.getAllAccounts()

            refreshAccountImages(accounts)

            Result.success()
        } catch (ex:Exception) {
            Timber.e(ex)
            Result.failure()
        }
    }

    private fun refreshAccountImages(accounts: List<Account>) {
        accounts
            .forEach { account ->

                val img = redditApiHelper.getAccountImageUrl(
                    redditAuthHelper.authClient(account).getSavedBearer().getAccessToken()!!
                )

                if(account.iconImage != img) {
                    Timber.d("updating Account.iconImage | old: ${account.iconImage} -> new: $img")

                    account.iconImage = img
                    dao.update(account)
                }
            }
    }
}