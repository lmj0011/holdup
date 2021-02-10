package name.lmj0011.redditdraftking.ui.submission.bottomsheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.database.models.Account

class BottomSheetViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }
}