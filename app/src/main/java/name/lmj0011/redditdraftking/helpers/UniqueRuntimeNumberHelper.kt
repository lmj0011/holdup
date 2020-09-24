package name.lmj0011.redditdraftking.helpers

import android.content.Context
import name.lmj0011.redditdraftking.App
import org.kodein.di.instance
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class UniqueRuntimeNumberHelper(val context: Context)  {
    companion object {
        const val INITIAL_NEXT_INT = 1
        const val INITIAL_NEXT_LONG = 1L
    }

    private val preferences: PreferencesHelper = (context.applicationContext as App).kodein.instance()
    private var intCounter: AtomicInteger = AtomicInteger(preferences.nextRuntimeUniqueInt())
    private var longCounter: AtomicLong = AtomicLong(preferences.nextRuntimeUniqueLong())

    /**
     * Get a unique Integer, should only be used for runtime uniqueness
     */
    fun nextInt(): Int {
        val cnt = intCounter.getAndIncrement()
        return when {
            (cnt < Int.MAX_VALUE) -> {
                preferences.nextRuntimeUniqueInt(cnt)
                cnt
            }
            else -> {
                preferences.nextRuntimeUniqueInt(INITIAL_NEXT_INT)
                INITIAL_NEXT_INT
            }
        }
    }

    /**
     * Get a unique Long, should only be used for runtime uniqueness
     */
    fun nextLong(): Long {
        val cnt = longCounter.getAndIncrement()
        return when {
            (cnt < Long.MAX_VALUE) -> {
                preferences.nextRuntimeUniqueLong(cnt)
                cnt
            }
            else -> {
                preferences.nextRuntimeUniqueLong(INITIAL_NEXT_LONG)
                INITIAL_NEXT_LONG
            }
        }
    }
}