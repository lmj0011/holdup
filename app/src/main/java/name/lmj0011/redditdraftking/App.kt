package name.lmj0011.redditdraftking

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import timber.log.Timber

class App: Application() {
    val kodein = DI.direct {
        bind<RedditAuthHelper>() with singleton { RedditAuthHelper(this@App) }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        AndroidThreeTen.init(this)
    }
}