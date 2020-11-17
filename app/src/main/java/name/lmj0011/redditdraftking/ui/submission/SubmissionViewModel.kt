package name.lmj0011.redditdraftking.ui.submission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import org.kodein.di.instance
import timber.log.Timber

class SubmissionViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()
    private var redditApiHelper: RedditApiHelper = (application as App).kodein.instance()

    private var account = MutableLiveData<Account>()
    private var subreddit = MutableLiveData<Subreddit>()
    private var subredditFlair = MutableLiveData<SubredditFlair?>()

    fun setAccount(acct: Account) {
        account.postValue(acct)
    }

    fun setSubreddit(sub: Subreddit) {
        subreddit.postValue(sub)
    }

    fun setSubredditFlair(flair: SubredditFlair?){
        subredditFlair.postValue(flair)
    }

    fun getSubredditFlairListFlow(subredditAndAccountPair: Pair<Subreddit, Account>): SharedFlow<List<SubredditFlair>> {
        return flow {
            val list = redditApiHelper.getSubredditFlairList(
                subredditAndAccountPair.first,
                redditAuthHelper.authClient(subredditAndAccountPair.second).getSavedBearer().getAccessToken()!!
            )
            emit(list)
        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getRecentSubredditListFlow(acct: Account): SharedFlow<List<Subreddit>> {
        return flow {
            val list = redditApiHelper.getRecentSubreddits(acct, redditAuthHelper.authClient(acct).getSavedBearer().getAccessToken()!!)
            emit(list)
        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getJoinedSubredditListFlow(acct: Account): SharedFlow<List<Subreddit>> {
        return flow {
            val list = redditApiHelper.getSubscribedSubreddits(redditAuthHelper.authClient(acct).getSavedBearer().getAccessToken()!!)
            emit(list)
        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getAccount(): LiveData<Account> {
        return account
    }

    fun getSubreddit(): LiveData<Subreddit> {
        return subreddit
    }

    fun getSubredditFlair(): LiveData<SubredditFlair?>{
        return subredditFlair
    }

    fun getSubredditAccountPair(): Pair<Subreddit, Account>? {
        val sub = subreddit.value
        val acct = account.value

        return if (sub != null && acct != null) {
            Pair(sub, acct)
        } else null
    }

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }
}