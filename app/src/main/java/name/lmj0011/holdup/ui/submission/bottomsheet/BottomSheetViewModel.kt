package name.lmj0011.holdup.ui.submission.bottomsheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Account

class BottomSheetViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }
}