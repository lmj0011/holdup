package name.lmj0011.holdup.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.holdup.database.SharedDao
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.util.RefreshableLiveData

class HomeViewModel(
    val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    var submissions = RefreshableLiveData { database.getAllSubmissionsObserverable() }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}