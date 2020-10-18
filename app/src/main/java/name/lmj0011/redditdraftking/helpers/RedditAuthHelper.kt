package name.lmj0011.redditdraftking.helpers

import android.content.Context
import com.kirkbushman.auth.RedditAuth
import com.kirkbushman.auth.managers.SharedPrefsStorageManager
import name.lmj0011.redditdraftking.BuildConfig
import name.lmj0011.redditdraftking.database.Account

class RedditAuthHelper(val context: Context) {

    companion object {
        const val DELAY_MILLIS = 1800000L // 30 minutes
    }


    private val baseBuilder = RedditAuth.Builder()
        // specify the credentials you can find on your reddit app console
        .setApplicationCredentials("T_694T2EB6g7UQ", "http://testapp.com/calback")
        // the api enpoints scopes this client will need
        .setScopes(arrayOf("submit", "read"))

    init {

        if (BuildConfig.DEBUG) baseBuilder.setLogging(true)
        else baseBuilder.setLogging(false)
    }

    fun authClient(account: Account? = null): RedditAuth {
        return if(account != null) {
            baseBuilder
                .setStorageManager(SharedPrefsStorageManager(context, account.id.toString()))
                .build()
        } else {
            baseBuilder
                .setStorageManager(SharedPrefsStorageManager(context))
                .build()
        }
    }

    /**
     * let's us know if we need the User to grant app access to their reddit account
     *
     * TODO - use a periodic Worker to keep Token refreshed
     */
//    val isAuthorized = flow {
//        if (authClient.hasSavedBearer()) {
//            when {
//                authClient.getSavedBearer().isAuthed() -> {
//                    emit(true)
//                }
//
//                authClient.getSavedBearer().isRevoked() -> {
//                    emit(false)
//                }
//
//                else -> {
//                    try {
//                        authClient.getSavedBearer().renewToken()
//                        emit(true)
//                    } catch (ex: Exception) {
//                        Timber.e(ex)
//                        emit(false)
//                    }
//                }
//            }
//        } else {
//            emit(false)
//        }
//    }.flowOn(Dispatchers.IO)


}