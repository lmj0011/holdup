package name.lmj0011.redditdraftking.ui.submission

import android.app.Application
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.SubmissionValidatorHelper
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.models.*
import name.lmj0011.redditdraftking.helpers.util.launchDefault
import name.lmj0011.redditdraftking.helpers.util.launchIO
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber
import java.io.File

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

    var submissionTitle = MutableLiveData<String?>()
        private set
    var subredditFlair = MutableLiveData<SubredditFlair?>()
        private set

    private var submissionLinkText = MutableLiveData<String?>()

    var submissionSelfText = MutableLiveData<String?>()
        private set

    var submissionImageGallery = MutableLiveData<MutableList<Image>?>()
        private set

    var submissionPollBodyText = MutableLiveData<String?>()
        private set
    var submissionPollOptions = MutableLiveData<List<String>?>()
        private set
    var submissionPollDuration = MutableLiveData<Int?>()
        private set

    var submissionVideo = MutableLiveData<Video?>()
        private set

    private var isNsfw = MutableLiveData(false)
    private var isSpoiler = MutableLiveData(false)
    private var readyToPost = MutableLiveData(false)

    var isSubmissionSuccessful = MutableLiveData(false)
        private set

    var recentAndJoinedSubredditPair = MutableLiveData(Pair(getRecentSubredditListFlow(), getJoinedSubredditListFlow()))
        private set

    fun setAccount(acct: Account) {
        account.postValue(acct)
    }

    fun setSubreddit(sub: Subreddit) {
        subreddit.postValue(sub)
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

    fun readyToPost(): LiveData<Boolean>{
        return readyToPost
    }

    fun getSubmissionLinkText(): LiveData<String?> {
        return submissionLinkText
    }

    fun getAccounts(): MutableList<Account> {
        return database.getAllAccounts()
    }

    /**
     * uploads a media file to Reddit and returns the url link
     */
    fun uploadMedia(uri: Uri): Pair<String, String>  {
        return redditApiHelper.uploadMedia(
            uri,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
    }

    /**
     * uploads a media file to Reddit and returns the url link
     */
    fun uploadMedia(file: File, mimeType: String): Pair<String, String>  {
        return redditApiHelper.uploadMedia(
            file,
            mimeType,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )
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
                        val form = getSubmissionForm(kind)
                        readyToPost.postValue(submissionValidatorHelper.validate(form, reqs))
                    }
                    SubmissionKind.Image -> {
                        val form = getSubmissionForm(kind)
                        readyToPost.postValue(submissionValidatorHelper.validate(form, reqs))
                    }
                    SubmissionKind.Video -> {
                        val form = getSubmissionForm(kind)
                        readyToPost.postValue(submissionValidatorHelper.validate(form, reqs))
                    }
                    SubmissionKind.VideoGif -> {
                        readyToPost.postValue(false)
                    }
                    SubmissionKind.Poll -> {
                        val form = getSubmissionForm(kind)
                        Timber.d("form: $form")
                        readyToPost.postValue(submissionValidatorHelper.validate(form, reqs))
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

            val json = JSONObject(res.body!!.string())
            Timber.d("$json")
            try {
                var url: String? = null

                when (kind) {
                    SubmissionKind.Link -> {
                        url = json.getJSONObject("json").getJSONObject("data").getString("url")
                        isSubmissionSuccessful.postValue(true)
                    }
                    SubmissionKind.Self -> {
                        url = json.getJSONObject("json").getJSONObject("data").getString("url")
                        isSubmissionSuccessful.postValue(true)
                    }
                    SubmissionKind.Image -> {
                        isSubmissionSuccessful.postValue(true)

                        if(form.images.size > 1) {
                            url = json.getJSONObject("json").getJSONObject("data").getString("url")
                        }
                    }
                    SubmissionKind.Video -> {
                        isSubmissionSuccessful.postValue(true)
                    }
                    SubmissionKind.Poll -> {
                        url = json.getJSONObject("json").getJSONObject("data").getString("url")
                        isSubmissionSuccessful.postValue(true)
                    }
                }

                NotificationHelper.showPostPublishedNotification(form, account.value!!.name, url)
            } catch(ex: JSONException) {
                Timber.e(ex)
                isSubmissionSuccessful.postValue(false)

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
                submissionTitle.value?.let { form.title = it }
                submissionLinkText.value?.let { form.url = it }
            }
            SubmissionKind.Self -> {
                form.kind = SubmissionKind.Self.kind
                submissionTitle.value?.let { form.title = it }
                submissionSelfText.value?.let { form.text = it }
            }
            SubmissionKind.Image -> {
                form.kind = SubmissionKind.Image.kind
                submissionTitle.value?.let { form.title = it }
                submissionImageGallery.value?.let { list ->
                    form.images = list.toList()

                    when {
                        (list.size == 1) -> {
                            form.url = list.first().url
                        }
                        (list.size > 1) -> {
                            form.items = getJsonArrayOfImages(form.images)
                        }
                    }


                }
            }
            SubmissionKind.Video -> {
                form.kind = SubmissionKind.Video.kind
                submissionTitle.value?.let { form.title = it }
                submissionVideo.value?.let {
                    form.url = it.url
                    form.video_poster_url = it.posterUrl
                }
            }
            SubmissionKind.VideoGif -> {
                form.kind = SubmissionKind.VideoGif.kind
                readyToPost.postValue(false)
            }
            SubmissionKind.Poll -> {
                form.kind = SubmissionKind.Poll.kind
                submissionTitle.value?.let { form.title = it }
                submissionPollBodyText.value?.let { form.text = it }
                submissionPollDuration.value?.let { form.duration = it }
                submissionPollOptions.value?.let { form.pollOptions = it }
                form.options = getJsonArrayOfOptions(form.pollOptions)
            }
        }

        subreddit.value?.let { form.sr = it.displayName }
        subredditFlair.value?.let { form.flair_id = it.id }
        subredditFlair.value?.let { form.flair_text = it.text }
        isNsfw.value?.let { form.nsfw = it}
        isSpoiler.value?.let { form.spoiler = it}

        return form
    }

    private fun getJsonArrayOfImages(images: List<Image>): JSONArray {
        val rootArray = JSONArray()

        images.forEach { image ->
            val jsonObj = JSONObject()
            jsonObj.put("media_id", image.mediaId)
            jsonObj.put("outbound_url", image.outboundUrl)
            jsonObj.put("caption", image.caption)

            rootArray.put(jsonObj)
        }

        Timber.d("${rootArray.toString(2)}")
        return rootArray
    }

    private fun getJsonArrayOfOptions(options: List<String>): JSONArray {
        val rootArray = JSONArray()

        options.forEach { opt ->
            rootArray.put(opt)
        }

        Timber.d("${rootArray.toString(2)}")
        return rootArray
    }


}