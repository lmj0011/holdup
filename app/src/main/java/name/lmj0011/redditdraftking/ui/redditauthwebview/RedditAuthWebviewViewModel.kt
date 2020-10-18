package name.lmj0011.redditdraftking.ui.redditauthwebview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.lmj0011.redditdraftking.database.Account
import name.lmj0011.redditdraftking.database.Draft
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.database.Subreddit
import name.lmj0011.redditdraftking.helpers.data.DraftJsonObject
import name.lmj0011.redditdraftking.helpers.data.SubredditJsonObject

class RedditAuthWebviewViewModel(
    val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO +  viewModelJob)
    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun createNewAccount(name: String? = ""): Account? {
        if(name.isNullOrBlank()) return null

        val newAcct = Account(name = name)
        database.upsert(newAcct)
        return database.getAccountByName(name)
    }

    fun deleteAccount(account: Account): Int {
        // TODO - also all relations; ie.) delete all associated and sharedPrefs created by reddit-oauth client
        return database.deleteByAccountName(account.name)
    }
}