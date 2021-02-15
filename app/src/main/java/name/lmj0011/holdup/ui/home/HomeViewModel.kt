package name.lmj0011.holdup.ui.home

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.lmj0011.holdup.database.models.Draft
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.database.models.Subreddit
import name.lmj0011.holdup.helpers.models.DraftJsonObject
import name.lmj0011.holdup.helpers.models.SubredditJsonObject
import java.time.Instant

class HomeViewModel(
    val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO +  viewModelJob)
    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()

    var drafts = database.getAllDraftsObserverable()
    var subreddits = database.getAllSubredditsObserverable()
    var subredditsWithDrafts = database.getSubredditWithDrafts()
    var submissions = database.getAllSubmissionsObserverable()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getSubmissions(): MutableList<Submission> {
        return database.getAllSubmissions()
    }

    @SuppressLint("NewApi")
    fun insertDraft(draftJsonObject: DraftJsonObject) {
        coroutineScope.launch {
            val subreddit = database.getSubreddit(draftJsonObject.subreddit)
            val draft = database.getDraft(draftJsonObject.id)

            if(draft is Draft){ // update existing Draft
                draft.apply {
                    kind = draftJsonObject.kind
                    title = draftJsonObject.title
                    dateModified = java.time.Instant.ofEpochMilli(draftJsonObject.modified).toString()
                    draftJsonObject.body?.let { body = it.toString() }

                    subreddit?.let {
                        subredditUuid = it.uuid
                    }

                    draftJsonObject.body?.let {
                        when {
                            it.first != null -> body = it.first.toString()
                            it.second != null -> body = it.second.toString()
                            else -> {}
                        }
                    }

                    draftJsonObject.flair?.let {
                        flairId = it.templateId
                        flairText = it.text
                    }
                }

                database.update(draft)
            } else { // create new Draft
                val draft = Draft(
                    uuid = draftJsonObject.id,
                    kind = draftJsonObject.kind,
                    title = draftJsonObject.title,
                    dateCreated = Instant.ofEpochMilli(draftJsonObject.created).toString(),
                    dateModified = Instant.ofEpochMilli(draftJsonObject.modified).toString()
                ).apply {
                    subreddit?.let {
                        subredditUuid = it.uuid
                    }

                    draftJsonObject.body?.let {
                        when {
                            it.first != null -> body = it.first.toString()
                            it.second != null -> body = it.second.toString()
                            else -> {}
                        }
                    }

                    draftJsonObject.flair?.let {
                        flairId = it.templateId
                        flairText = it.text
                    }
                }
                database.insert(draft)
            }
        }
    }

    fun insertSubreddit(subredditJsonObject: SubredditJsonObject) {
        coroutineScope.launch {
            val subreddit = database.getSubreddit(subredditJsonObject.name)

            if(subreddit is Subreddit){
                subreddit.apply {
                    displayName = subredditJsonObject.display_name
                    displayNamePrefixed = subredditJsonObject.display_name_prefixed
                    url = subredditJsonObject.url
                    iconImgUrl = if (subredditJsonObject.icon_img.isNullOrBlank()) {
                        subredditJsonObject.community_icon
                    } else subredditJsonObject.icon_img
                }
                database.update(subreddit)
            } else { // create new Subreddit
                val subreddit = Subreddit(
                    uuid = subredditJsonObject.name,
                    displayName = subredditJsonObject.display_name,
                    displayNamePrefixed = subredditJsonObject.display_name_prefixed,
                    url = subredditJsonObject.url,
                    iconImgUrl = subredditJsonObject.icon_img
                )
                database.insert(subreddit)
            }


        }

    }
}