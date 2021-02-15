package name.lmj0011.holdup.helpers

import android.content.Context
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
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
        val intCounter = AtomicInteger(cnt)

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
        val longCounter = AtomicLong(cnt)

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