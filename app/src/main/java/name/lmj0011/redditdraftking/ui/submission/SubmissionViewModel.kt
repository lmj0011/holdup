package name.lmj0011.redditdraftking.ui.submission

import android.app.Application
import androidx.annotation.MainThread
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
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.SubmissionValidatorHelper
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.models.PostRequirements
import name.lmj0011.redditdraftking.helpers.models.Subreddit
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import name.lmj0011.redditdraftking.helpers.util.launchDefault
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchNow
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class SubmissionViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {

    // ref: https://stackoverflow.com/a/61918988/2445763
    companion object {
        private lateinit var instance: SubmissionViewModel

        @MainThread
        fun getInstance(database: SharedDao, application: Application): SubmissionViewModel {
            instance = if (::instance.isInitialized) instance
            else SubmissionViewModel(database, application)
            
            return instance
        }
    }

    private var redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()
    private var redditApiHelper: RedditApiHelper = (application as App).kodein.instance()
    private var submissionValidatorHelper: SubmissionValidatorHelper = (application as App).kodein.instance()

    private var account = MutableLiveData<Account>()
    private var subreddit = MutableLiveData<Subreddit>()
    private var subredditPostRequirements = MutableLiveData<PostRequirements?>()
    private var subredditFlair = MutableLiveData<SubredditFlair?>()
    private var submissionLinkText = MutableLiveData<String?>()
    private var submissionLinkTitle = MutableLiveData<String?>()
    private var isNsfw = MutableLiveData(false)
    private var isSpoiler = MutableLiveData(false)
    private var readyToPost = MutableLiveData(false)

    var isLinkSubmissionSuccessful = MutableLiveData(false)
     private set

    var recentAndJoinedSubredditPair = MutableLiveData(Pair(getRecentSubredditListFlow(), getJoinedSubredditListFlow()))
     private set

    fun setAccount(acct: Account) {
        account.postValue(acct)
    }

    fun setSubreddit(sub: Subreddit) {
        subreddit.postValue(sub)
    }

    fun setSubredditFlair(flair: SubredditFlair?){
        subredditFlair.postValue(flair)
    }

    fun setSubmissionLinkTitle(s: String) {
        submissionLinkTitle.postValue(s)
    }

    fun setSubmissionLinkText(s: String) {
        submissionLinkText.postValue(s)
    }

    fun setSubredditPostRequirements(sub: Subreddit) {
        launchIO {
            val postRequirements = redditApiHelper.getPostRequirements(
                sub,
                redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
            )

            subredditPostRequirements.postValue(postRequirements)
        }
    }

    fun setNsfwFlag(nsfw: Boolean) {
        isNsfw.postValue(nsfw)
    }

    fun setSpoilerFlag(spoiler: Boolean) {
        isSpoiler.postValue(spoiler)
    }

    fun getSubredditFlairListFlow(): SharedFlow<List<SubredditFlair>> {
        return flow {
            val list = redditApiHelper.getSubredditFlairList(
                subreddit.value!!,
                redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
            )
            emit(list)
        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getRecentSubredditListFlow(): SharedFlow<List<Subreddit>> {
        return flow {
            val acct  = account.value

            if(acct != null) {
                val list = redditApiHelper.getRecentSubreddits(acct, redditAuthHelper.authClient(acct).getSavedBearer().getAccessToken()!!)
                emit(list)
            } else {
                emit(mutableListOf<Subreddit>())
            }

        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getJoinedSubredditListFlow(): SharedFlow<List<Subreddit>> {
        return flow {
            val acct  = account.value

            if(acct != null) {
                val list = redditApiHelper.getSubscribedSubreddits(redditAuthHelper.authClient(acct).getSavedBearer().getAccessToken()!!)
                emit(list)
            } else {
                emit(mutableListOf<Subreddit>())
            }
        }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 1)
    }

    fun getAccount(): LiveData<Account> {
        return account
    }

    fun getSubreddit(): LiveData<Subreddit> {
        return subreddit
    }

    fun getSubredditPostRequirements(): LiveData<PostRequirements?> {
        return subredditPostRequirements
    }

    fun getSubredditFlair(): LiveData<SubredditFlair?>{
        return subredditFlair
    }

    fun readyToPost(): LiveData<Boolean>{
        return readyToPost
    }

    fun getSubredditAccountPair(): Pair<Subreddit, Account>? {
        val sub = subreddit.value
        val acct = account.value

        return if (sub != null && acct != null) {
            Pair(sub, acct)
        } else null
    }

    fun getSubmissionLinkTitle(): LiveData<String?> {
        return submissionLinkTitle
    }

    fun getSubmissionLinkText(): LiveData<String?> {
        return submissionLinkText
    }

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }

    /**
     * validates the data (determined by [kind]) currently stored in this viewModel
     *
     * updates [readyToPost]
     *
     * [kind] should be one of (link, self, image, video, videogif, poll)
     */
    fun validateSubmission(kind: SubmissionKind) {
        launchDefault {
            val sub = subreddit.value
            val acct = account.value
            val reqs = subredditPostRequirements.value

            if(sub != null && acct != null && reqs != null) {
                when (kind) {
                    SubmissionKind.Link -> {
                        val form = getSubmissionForm(kind)
                        readyToPost.postValue(submissionValidatorHelper.validate(form, reqs))
                    }
                    SubmissionKind.Self -> {
                        readyToPost.postValue(false)
                    }
                    SubmissionKind.Image -> {
                        readyToPost.postValue(false)
                    }
                    SubmissionKind.Video -> {
                        readyToPost.postValue(false)
                    }
                    SubmissionKind.VideoGif -> {
                        readyToPost.postValue(false)
                    }
                    SubmissionKind.Poll -> {
                        readyToPost.postValue(false)
                    }
                }
            }
        }
    }

    fun postSubmission(kind: SubmissionKind) {
        val form = getSubmissionForm(kind)

        launchIO {
            val res = redditApiHelper.submit(form,
                redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
            )

            try {
                val json = JSONObject(res.body!!.string())
                val url = json.getJSONObject("json").getJSONObject("data").getString("url")
                isLinkSubmissionSuccessful.postValue(true)
                NotificationHelper.showPostPublishedNotification(form, url)
            } catch(ex: JSONException) {
                isLinkSubmissionSuccessful.postValue(false)
                val json = JSONObject(res.body!!.string())

                Timber.e(ex)
                Timber.d("$json")

                val errors = json.getJSONObject("json").getJSONArray("errors")

                if (errors.length() > 0) {
                    val arr = errors.getJSONArray(0)
                    val errorMessage = "error: ${arr.join(" ").replace("\"", "")}"
                    Timber.d("$errorMessage")
                    /** TODO - throw custom Exception */
                } else throw ex

            }
        }
    }

    private fun getSubmissionForm(kind: SubmissionKind): SubmissionValidatorHelper.SubmissionForm {
        val form = SubmissionValidatorHelper.SubmissionForm()

        when (kind) {
            SubmissionKind.Link -> {
                form.kind = SubmissionKind.Link.kind
                submissionLinkTitle.value?.let { form.title = it }
                submissionLinkText.value?.let { form.url = it }
            }
            SubmissionKind.Self -> {
                form.kind = SubmissionKind.Self.kind
                readyToPost.postValue(false)
            }
            SubmissionKind.Image -> {
                form.kind = SubmissionKind.Image.kind
                readyToPost.postValue(false)
            }
            SubmissionKind.Video -> {
                form.kind = SubmissionKind.Video.kind
                readyToPost.postValue(false)
            }
            SubmissionKind.VideoGif -> {
                form.kind = SubmissionKind.VideoGif.kind
                readyToPost.postValue(false)
            }
            SubmissionKind.Poll -> {
                form.kind = SubmissionKind.Poll.kind
                readyToPost.postValue(false)
            }
        }

        subreddit.value?.let { form.sr = it.displayName }
        subredditFlair.value?.let { form.flair_id = it.id }
        subredditFlair.value?.let { form.flair_text = it.text }
        isNsfw.value?.let { form.nsfw = it}
        isSpoiler.value?.let { form.spoiler = it}

        return form
    }


}