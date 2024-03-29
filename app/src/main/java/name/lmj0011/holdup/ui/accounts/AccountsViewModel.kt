package name.lmj0011.holdup.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.helpers.RedditAuthHelper
import org.kodein.di.instance

class AccountsViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    private val redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }

    fun deleteAccount(account: Account){
        val client = redditAuthHelper.authClient(account)

        try {
            if(client.hasSavedBearer() && !client.getSavedBearer().isRevoked()) {
                client.getSavedBearer().revokeToken()
            }
        } catch (ex: java.lang.IllegalStateException) {
            // do nothing
        }
        finally {
            database.deleteByAccountId(account.id)
        }
    }
}