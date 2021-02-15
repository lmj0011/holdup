package name.lmj0011.holdup.ui.subredditdrafts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.holdup.database.models.Draft
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Subreddit

class SubredditDraftsViewModel(
    private val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val corScope = CoroutineScope(Dispatchers.IO +  viewModelJob)
    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getSubreddit(subredditUuid: String): Subreddit? {
        return database.getSubreddit(subredditUuid)
    }

    fun getDrafts(subredditUuid: String): MutableList<Draft> {
        return database.getAllDraftsBySubreddit(subredditUuid)
    }

    fun updateDraft(draft: Draft) {
        return database.update(draft)
    }
}