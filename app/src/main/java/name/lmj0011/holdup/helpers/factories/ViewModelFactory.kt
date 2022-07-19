package name.lmj0011.holdup.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.holdup.database.BaseDao
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.ui.accounts.AccountsViewModel
import name.lmj0011.holdup.ui.home.HomeViewModel
import name.lmj0011.holdup.ui.redditauthwebview.RedditAuthWebviewViewModel
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetViewModel
import name.lmj0011.holdup.ui.testing.TestingViewModel

class ViewModelFactory(
    private val dataSource: BaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(RedditAuthWebviewViewModel::class.java) -> {
                RedditAuthWebviewViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(TestingViewModel::class.java) -> {
                TestingViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(BottomSheetViewModel::class.java) -> {
                BottomSheetViewModel(dataSource as SharedDao, application) as T
            }

            modelClass.isAssignableFrom(SubmissionViewModel::class.java) -> {
                SubmissionViewModel(dataSource as SharedDao, application) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}