package name.lmj0011.redditdraftking.helpers

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.toList
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchUI
import org.kodein.di.instance
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class UniqueRuntimeNumberHelper(val context: Context)  {
    companion object {
        const val INITIAL_NEXT_INT = 1
        const val INITIAL_NEXT_LONG = 1L
    }

    private val dataStoreHelper: DataStoreHelper = (context.applicationContext as App).kodein.instance()

    /**
     * Get a unique Integer, should only be used for runtime uniqueness
     */
    suspend fun nextInt(): Int {
        val cnt = dataStoreHelper.getNextRuntimeUniqueInt().conflate().first()
        val intCounter = if (cnt != null) AtomicInteger(cnt) else AtomicInteger(INITIAL_NEXT_INT)

        val value = intCounter.incrementAndGet()
        return when {
            (value < Int.MAX_VALUE) -> {
                dataStoreHelper.setNextRuntimeUniqueInt(value)
                value
            }
            else -> {
                dataStoreHelper.setNextRuntimeUniqueInt(INITIAL_NEXT_INT)
                INITIAL_NEXT_INT
            }
        }
    }

    /**
     * Get a unique Long, should only be used for runtime uniqueness
     */
    suspend fun nextLong(): Long {
        val cnt = dataStoreHelper.getNextRuntimeUniqueLong().conflate().first()
        val longCounter = if (cnt != null) AtomicLong(cnt) else AtomicLong(INITIAL_NEXT_LONG)

        val value = longCounter.incrementAndGet()
        return when {
            (value < Long.MAX_VALUE) -> {
                dataStoreHelper.setNextRuntimeUniqueLong(value)
                value
            }
            else -> {
                dataStoreHelper.setNextRuntimeUniqueLong(INITIAL_NEXT_LONG)
                INITIAL_NEXT_LONG
            }
        }
    }
}