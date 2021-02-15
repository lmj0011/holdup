package name.lmj0011.holdup.ui.redditauthwebview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.SharedDao

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

    fun createNewAccount(name: String = "", iconImage: String = ""): Account? {
        if(name.isBlank()) return null

        val newAcct = Account(name = name, iconImage = iconImage)
        database.upsert(newAcct)
        return database.getAccountByName(name)
    }

    fun updateAccount(acct: Account) {
        database.update(acct)
    }

    fun deleteAccount(account: Account): Int {
        // TODO - also all relations; ie.) delete all associated and sharedPrefs created by reddit-oauth client
        return database.deleteByAccountName(account.name)
    }
}