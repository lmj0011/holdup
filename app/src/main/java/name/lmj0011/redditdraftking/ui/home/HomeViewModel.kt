package name.lmj0011.redditdraftking.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.lmj0011.redditdraftking.database.Draft
import name.lmj0011.redditdraftking.database.SharedDao
import name.lmj0011.redditdraftking.database.Subreddit
import name.lmj0011.redditdraftking.helpers.data.DraftJsonObject
import name.lmj0011.redditdraftking.helpers.data.SubredditJsonObject

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

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun insertDraft(draftJsonObject: DraftJsonObject) {
        coroutineScope.launch {
            val subreddit = database.getSubreddit(draftJsonObject.subreddit)
            val draft = database.getDraft(draftJsonObject.id)

            if(draft is Draft){ // update existing Draft
                draft.apply {
                    kind = draftJsonObject.kind
                    title = draftJsonObject.title
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