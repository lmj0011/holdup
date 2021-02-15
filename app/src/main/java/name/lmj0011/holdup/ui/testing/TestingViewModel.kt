package name.lmj0011.holdup.ui.testing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.database.SharedDao

class TestingViewModel(
    val database: SharedDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO +  viewModelJob)
    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getFirstAccount(): Account? {
        return database.getAccount(1L)
    }

}