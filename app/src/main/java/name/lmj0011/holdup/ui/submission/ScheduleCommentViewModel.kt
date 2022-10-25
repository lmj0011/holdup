package name.lmj0011.holdup.ui.submission

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

class ScheduleCommentViewModel (
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    // ref: https://stackoverflow.com/a/61918988/2445763
    companion object {
        private lateinit var instance: ScheduleCommentViewModel

        @MainThread
        fun getInstance(database: SharedDao, application: Application): ScheduleCommentViewModel {
            instance = if (::instance.isInitialized) instance
            else ScheduleCommentViewModel(database, application)

            return instance
        }

        @MainThread
        fun getNewInstance(database: SharedDao, application: Application): ScheduleCommentViewModel {
            instance = ScheduleCommentViewModel(database, application)
            return instance
        }
    }

    private var redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()
    private var redditApiHelper: RedditApiHelper = (application as App).kodein.instance()
    private var dataStoreHelper: DataStoreHelper = (application as App).kodein.instance()

    private var _commenterComment = MutableSharedFlow<Thing1>(1)
    private var _commenterImgUrl = MutableSharedFlow<String>(1)
    private var _post = MutableSharedFlow<Thing3>(1)
    private var _postSubredditImgUrl = MutableSharedFlow<String>(1)
    private var _account = MutableSharedFlow<Account>(1)

    /**
     * Flows available to subscribers
     */
    val commenterComment: SharedFlow<Thing1>
        get() = _commenterComment

    val commenterImgUrl: SharedFlow<String>
        get() = _commenterImgUrl

    val post: SharedFlow<Thing3>
        get() = _post

    val postSubredditImgUrl: SharedFlow<String>
        get() = _postSubredditImgUrl

    val account: SharedFlow<Account>
        get() = _account
    /***/

    /**
     * Sets the first Account in the db as the active account for Submissions if
     * there's not an Account set in DataStore
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun setAccount() {
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

        _account.tryEmit(acct)
    }

    fun setAccount(acct: Account) {
        _account.tryEmit(acct)
    }

    suspend fun getPostById(fullName: String): Thing3 {
        val thing = redditApiHelper.getPostById(
            fullName,
            redditAuthHelper.authClient(_account.first()!!).getSavedBearer().getAccessToken()!!
        )

        _post.tryEmit(thing)

        val subreddit = redditApiHelper.getSubredditById(
            thing.subredditId,
            redditAuthHelper.authClient(_account.first()!!).getSavedBearer().getAccessToken()!!
        )

        _postSubredditImgUrl.tryEmit(subreddit.iconImgUrl)

        return thing
    }

    /**
     * Fetches a Comment by it's fullname, and also fetches the Commenter's avatar image in another thread due
     * to it not being included in the same payload..sigh. The value is emitted into the [commenterImgUrl] Flow
     */
    suspend fun getCommentById(fullName: String, subredditNamePrefixed: String): Thing1 {
        val thing = redditApiHelper.getCommentById(
            fullName,
            subredditNamePrefixed,
            redditAuthHelper.authClient(_account.first()).getSavedBearer().getAccessToken()!!
        )
        _commenterComment.tryEmit(thing)

        val url = redditApiHelper.getOtherUserImageUrl(
            thing.author,
            redditAuthHelper.authClient(_account.first()).getSavedBearer().getAccessToken()!!
        )

        _commenterImgUrl.tryEmit(url)

        return thing
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetFlows() {
        _commenterComment.resetReplayCache()
        _commenterImgUrl.resetReplayCache()
        _post.resetReplayCache()
        _postSubredditImgUrl.resetReplayCache()
        _account.resetReplayCache()
    }

}