package name.lmj0011.holdup.ui.patton

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.models.Thing1
import name.lmj0011.holdup.helpers.models.Thing3
import name.lmj0011.holdup.helpers.util.launchIO
import okhttp3.Response
import org.kodein.di.instance

class PattonViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    // ref: https://stackoverflow.com/a/61918988/2445763
    companion object {
        private lateinit var instance: PattonViewModel

        @MainThread
        fun getInstance(database: SharedDao, application: Application): PattonViewModel {
            instance = if (::instance.isInitialized) instance
            else PattonViewModel(database, application)

            return instance
        }

        @MainThread
        fun getNewInstance(database: SharedDao, application: Application): PattonViewModel {
            instance = PattonViewModel(database, application)
            return instance
        }
    }

    private var redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()
    private var redditApiHelper: RedditApiHelper = (application as App).kodein.instance()
    private var dataStoreHelper: DataStoreHelper = (application as App).kodein.instance()
    private var account = MutableLiveData<Account>()

    /**
     * Sets the first Account in the db as the active account for Submissions if
     * there's not an Account set in DataStore
     */
    fun setAccount() {
        launchIO {
            val accounts = database.getAllAccounts()
            val username = dataStoreHelper.getSelectedAccountUsername().first()

            val lastSelected = accounts.filter {
                it.name == username
            }

            val acct = when {
                lastSelected.isNotEmpty() -> lastSelected.first()
                accounts.isNotEmpty() -> accounts.first()
                else -> throw Exception("There are no signed in Reddit Accounts")
            }

            account.postValue(acct)
        }
    }

    fun setAccount(acct: Account) {
        account.postValue(acct)
    }

    fun getAccount(): LiveData<Account> {
        return account
    }

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }

    fun getPostById(fullName: String): Thing3 {
        return redditApiHelper.getPostById(
            fullName,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }

    fun getCommentById(fullName: String, subredditNamePrefixed: String): Thing1 {
        return redditApiHelper.getCommentById(
            fullName,
            subredditNamePrefixed,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }

    fun upvoteThing(fullName: String): Response {
        return redditApiHelper.castVote(
            fullName,
            1,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }
}