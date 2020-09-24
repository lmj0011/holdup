package name.lmj0011.redditdraftking.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.redditdraftking.database.BaseDao
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.ui.home.HomeViewModel
import name.lmj0011.redditdraftking.ui.subredditdrafts.SubredditDraftsViewModel

class ViewModelFactory(
    private val dataSource: BaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(dataSource as SharedDao, application) as T
        }

        if (modelClass.isAssignableFrom(SubredditDraftsViewModel::class.java)) {
            return SubredditDraftsViewModel(dataSource as SharedDao, application) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}