package name.lmj0011.redditdraftking.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.redditdraftking.database.BaseDao
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.ui.accounts.AccountsViewModel
import name.lmj0011.redditdraftking.ui.home.HomeViewModel
import name.lmj0011.redditdraftking.ui.redditauthwebview.RedditAuthWebviewViewModel
import name.lmj0011.redditdraftking.ui.subredditdrafts.SubredditDraftsViewModel

class ViewModelFactory(
    private val dataSource: BaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(SubredditDraftsViewModel::class.java) -> {
                SubredditDraftsViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(RedditAuthWebviewViewModel::class.java) -> {
                RedditAuthWebviewViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(dataSource as SharedDao, application) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}