package name.lmj0011.redditdraftking.helpers

import android.content.Context
import com.kirkbushman.auth.RedditAuth
import com.kirkbushman.auth.managers.SharedPrefsStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.lang.Exception

class RedditAuthHelper(context: Context) {

    companion object {
        const val DELAY_MILLIS = 1800000L // 30 minutes
    }

    var authClient: RedditAuth
     private set

    init {
        authClient = RedditAuth.Builder()
            // specify the credentials you can find on your reddit app console
            .setApplicationCredentials("T_694T2EB6g7UQ", "http://testapp.com/calback")
            // the api enpoints scopes this client will need
            .setScopes(arrayOf("submit", "read"))
            // to manage tokens info in memory
            .setStorageManager(SharedPrefsStorageManager(context))
            // if you set this flag to 'true' it will add to the OkHttp Client a listener to log the
            // Request and Response object, to make it easy to debug.
            .setLogging(true)
            .build()
    }

    /**
     * let's us know if we need the User to grant app access to their reddit account
     */
    val isAuthorized = flow {
        while (true) {
            if (authClient.hasSavedBearer()) {
                when {
                    authClient.getSavedBearer().isAuthed() -> {
                        emit(true)
                    }

                    authClient.getSavedBearer().isRevoked() -> {
                        emit(false)
                    }

                    else -> {
                        try {
                            authClient.getSavedBearer().renewToken()
                            emit(true)
                        } catch (ex: Exception) {
                            Timber.e(ex)
                            emit(false)
                        }
                    }
                }
            } else {
                emit(false)
            }

            delay(DELAY_MILLIS)
        }

    }.flowOn(Dispatchers.IO)


}