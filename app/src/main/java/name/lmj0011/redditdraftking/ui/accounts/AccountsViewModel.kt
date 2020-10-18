package name.lmj0011.redditdraftking.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.database.Account
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
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
        if(client.hasSavedBearer()) {
            client.getSavedBearer().revokeToken()
        }

        database.deleteByAccountId(account.id)
    }
}