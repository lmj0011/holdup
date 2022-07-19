package name.lmj0011.holdup.helpers

import android.content.Context
import com.kirkbushman.auth.RedditAuth
import com.kirkbushman.auth.managers.SharedPrefsStorageManager
import name.lmj0011.holdup.BuildConfig
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Account

class RedditAuthHelper(val context: Context) {
    private val baseBuilder = RedditAuth.Builder()
        // specify the credentials you can find on your reddit app console
        .setApplicationCredentials(
            context.getString(R.string.reddit_app_clientId),
            context.getString(R.string.reddit_app_redirectUri)
        )
        // the api enpoints scopes this client will need
        .setScopes(arrayOf("submit", "read", "mysubreddits", "history", "flair", "vote"))

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

}