package name.lmj0011.redditdraftking.helpers

import android.content.Context
import kotlinx.coroutines.flow.collectLatest
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
    private lateinit var intCounter: AtomicInteger
    private lateinit var longCounter: AtomicLong

    init {
        launchIO {
            dataStoreHelper.getNextRuntimeUniqueInt().collectLatest { cnt ->
                cnt?.let { intCounter = AtomicInteger(it) }
            }

            dataStoreHelper.getNextRuntimeUniqueLong().collectLatest { cnt ->
                cnt?.let { longCounter = AtomicLong(it) }
            }
        }
    }

    /**
     * Get a unique Integer, should only be used for runtime uniqueness
     */
    fun nextInt(): Int {
        val cnt = intCounter.getAndIncrement()
        return when {
            (cnt < Int.MAX_VALUE) -> {
                launchIO { dataStoreHelper.setNextRuntimeUniqueInt(cnt) }
                cnt
            }
            else -> {
                launchIO { dataStoreHelper.setNextRuntimeUniqueInt(INITIAL_NEXT_INT) }
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
                launchIO { dataStoreHelper.setNextRuntimeUniqueLong(cnt) }
                cnt
            }
            else -> {
                launchIO { dataStoreHelper.setNextRuntimeUniqueLong(INITIAL_NEXT_LONG) }
                INITIAL_NEXT_LONG
            }
        }
    }
}