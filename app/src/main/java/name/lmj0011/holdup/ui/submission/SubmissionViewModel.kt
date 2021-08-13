package name.lmj0011.holdup.ui.submission

import android.app.Application
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.SubmissionValidatorHelper
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.models.PostRequirements
import name.lmj0011.holdup.helpers.models.Subreddit
import name.lmj0011.holdup.helpers.models.SubredditFlair
import name.lmj0011.holdup.helpers.models.Video
import name.lmj0011.holdup.helpers.util.launchDefault
import name.lmj0011.holdup.helpers.util.launchIO
import org.json.JSONArray
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

        @MainThread
        fun getNewInstance(database: SharedDao, application: Application): SubmissionViewModel {
            instance = SubmissionViewModel(database, application)
            return instance
        }
    }

    private var redditAuthHelper: RedditAuthHelper = (application as App).kodein.instance()
    private var redditApiHelper: RedditApiHelper = (application as App).kodein.instance()
    private var submissionValidatorHelper: SubmissionValidatorHelper = (application as App).kodein.instance()
    private var dataStoreHelper: DataStoreHelper = (application as App).kodein.instance()

    private var account = MutableLiveData<Account>()
    private var subreddit = MutableLiveData<Subreddit>()
    private var subredditPostRequirements = MutableLiveData<PostRequirements?>()

    var submissionTitle = MutableLiveData<String?>()
        private set
    var subredditFlair = MutableLiveData<SubredditFlair?>()
        private set

    var submissionLinkText = MutableLiveData<String?>()
        private set

    var submissionLinkImageUrl = MutableLiveData<String?>()
        private set

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

    var sendReplies = MutableLiveData(true)
        private set

    private var isNsfw = MutableLiveData(false)
    private var isSpoiler = MutableLiveData(false)
    private var readyToPost = MutableLiveData(false)

    var isSubmissionSuccessful = MutableLiveData(false)
        private set

    var recentAndJoinedSubredditPair = MutableLiveData(Pair(getRecentSubredditListFlow(), getJoinedSubredditListFlow()))
        private set

    init {
        launchIO {
            sendReplies.postValue(dataStoreHelper.getEnableInboxReplies().first())
        }
    }

    /**
     * Sets the first Account in the db as the active account for Submissions if
     * there's not an Account set in DataStore
     */
    fun setAccount() {
        launchIO {
            val accounts = database.getAllAccounts()
            val username = dataStoreHelper.getSelectedAccountUsername().first()

            val lastSelected = accounts.filter {
                it.name == username
            }

            when {
                lastSelected.isNotEmpty() -> account.postValue(lastSelected.first())
                accounts.isNotEmpty() -> account.postValue(accounts.first())
            }
        }
    }

    fun setAccount(acct: Account) {
        Timber.d("account: $acct")
        account.postValue(acct)
    }

    fun setSubreddit(sub: Subreddit) {
        Timber.d("subreddit: $sub")
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

    fun toggleSendReplies() {
        val enable = !sendReplies.value!!
        launchIO {
            dataStoreHelper.setEnableInboxReplies(enable)
        }

        sendReplies.postValue(enable)
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

    /**
     * Post to Reddit, data needed for the Post comes from this viewModel's LiveData
     *
     * returns a Pair<jsonData, errorMessage>
     */
    fun postSubmission(kind: SubmissionKind): Pair<JSONObject?, String?> {
        val form = getSubmissionForm(kind)

        val res = redditApiHelper.submit(form,
            redditAuthHelper.authClient(account.value!!).getSavedBearer().getAccessToken()!!
        )

        val responsePair = RedditApiHelper.parseSubmitResponse(JSONObject(res.body!!.source().readUtf8()))

        if (responsePair.first != null && responsePair.second == null) { // successful response
            when (kind) {
                SubmissionKind.Image -> {
                    if(form.images.size > 1) {
                        NotificationHelper.showSubmissionPublishedNotification(
                            subreddit.value!!,
                            account.value!!,
                            form,
                            responsePair.first!!.getString("url")
                        )
                    } else {
                        NotificationHelper.showSubmissionPublishedNotification(
                            subreddit.value!!,
                            account.value!!,
                            form,
                            null
                        )
                    }
                }
                SubmissionKind.Video -> {
                    NotificationHelper.showSubmissionPublishedNotification(
                        subreddit.value!!,
                        account.value!!,
                        form,
                        null
                    )
                }
                else -> {
                    NotificationHelper.showSubmissionPublishedNotification(
                        subreddit.value!!,
                        account.value!!,
                        form,
                        responsePair.first!!.getString("url")
                    )
                }
            }
            isSubmissionSuccessful.postValue(true)
        }

        return responsePair
    }

    fun saveSubmission(kind: SubmissionKind, postAtMillis: Long, alarmRequestCode: Int) {
        val submission = Submission(kind = kind, postAtMillis = postAtMillis, alarmRequestCode = alarmRequestCode)

        submissionTitle.value?.let { submission.title = it }

        isNsfw.value?.let { submission.isNsfw = it}
        isSpoiler.value?.let { submission.isSpoiler = it }

        subredditFlair.value?.let { submission.subredditFlair = it }

        if(kind == SubmissionKind.Self) submissionSelfText.value?.let { submission.body = it }

        submissionLinkText.value?.let { submission.url = it }
        submissionLinkImageUrl.value?.let { submission.linkImageUrl = it }

        submissionImageGallery.value?.let { submission.imgGallery = it }

        if(kind == SubmissionKind.Poll) submissionPollBodyText.value?.let { submission.body = it }

        submissionPollOptions.value?.let { submission.pollOptions = it.toMutableList() }
        submissionPollDuration.value?.let { submission.pollDuration = it }

        submissionVideo.value?.let { submission.video = it }

        subreddit.value?.let { submission.subreddit = it }

        account.value?.let { submission.account = it }

        database.insert(submission)
    }

    fun updateSubmission(submission: Submission) {
        submissionTitle.value?.let { submission.title = it }

        isNsfw.value?.let { submission.isNsfw = it}
        isSpoiler.value?.let { submission.isSpoiler = it }

        subredditFlair.value?.let { submission.subredditFlair = it }

        /**
         * We save this Submission when it's updated in TestSubmissionFragment to avoid a weird bug where
         * this property doesn't ever update beyond it's initial value
         */
//        if(submission.kind == SubmissionKind.Self) submissionSelfText.value?.let { submission.body = it }

        submissionLinkText.value?.let { submission.url = it }
        submissionLinkImageUrl.value?.let { submission.linkImageUrl = it }

        submissionImageGallery.value?.let { submission.imgGallery = it }

        if(submission.kind == SubmissionKind.Poll) submissionPollBodyText.value?.let { submission.body = it }

        submissionPollOptions.value?.let { submission.pollOptions = it.toMutableList() }
        submissionPollDuration.value?.let { submission.pollDuration = it }

        submissionVideo.value?.let { submission.video = it }

        subreddit.value?.let { submission.subreddit = it }

        account.value?.let { submission.account = it }

        database.update(submission)
    }

    fun deleteSubmission(submission: Submission) {

        database.deleteBySubmissionId(submission.id)
    }

    /**
     * Takes a Submission and configure this viewModel to reflect
     * its data.
     *
     * returns a Pair<jsonData, errorMessage>
     */
    fun populateFromSubmissionThenPost(sub: Submission, postNow: Boolean = true): Pair<JSONObject?, String?>? {

        account.postValue(sub.account)
        subreddit.postValue(sub.subreddit)
        submissionTitle.postValue(sub.title)
        subredditFlair.postValue(sub.subredditFlair)
        submissionLinkText.postValue(sub.url)
        submissionLinkImageUrl.postValue(sub.linkImageUrl)

        if(sub.kind == SubmissionKind.Self) submissionSelfText.postValue(sub.body)

        submissionImageGallery.postValue(sub.imgGallery)

        if(sub.kind == SubmissionKind.Poll) submissionPollBodyText.postValue(sub.body)
        submissionPollOptions.postValue(sub.pollOptions)
        submissionPollDuration.postValue(sub.pollDuration)
        submissionVideo.postValue(sub.video)

        isNsfw.postValue(sub.isNsfw)
        isSpoiler.postValue(sub.isSpoiler)

        val kind = sub.kind

        return if (postNow && kind != null) {
            postSubmission(kind)
        } else null
    }

    private fun getSubmissionForm(kind: SubmissionKind): SubmissionValidatorHelper.SubmissionForm {
        val form = SubmissionValidatorHelper.SubmissionForm()

        when (kind) {
            SubmissionKind.Link -> {
                form.kind = SubmissionKind.Link.kind
                submissionTitle.value?.let { form.title = it }
                submissionLinkText.value?.let { form.url = it }
                submissionLinkImageUrl.value?.let { form.linkImageUrl = it }
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
        sendReplies.value?.let { form.sendreplies = it }

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