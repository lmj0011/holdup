package name.lmj0011.holdup.helpers

import android.os.SystemClock
import name.lmj0011.holdup.database.models.Submission
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object DateTimeHelper {

    /**
     * Get the elapsed time until a moment in the future
     * [futureTimeInMillis] should be UTC
     */
    fun getElapsedTimeUntilFutureTime(futureTimeInMillis: Long): Long {
        val rightNowTime = Calendar.getInstance().timeInMillis
        val systemElapsedTime = SystemClock.elapsedRealtime()

        Timber.d("futureTimeInMillis: $futureTimeInMillis")
        Timber.d("rightNowTime: $rightNowTime")
        Timber.d("systemElapsedTime: $systemElapsedTime")

        val elapsedTimeToPublish = (futureTimeInMillis - rightNowTime) + systemElapsedTime

        Timber.d("elapsedTimeToPublish: $elapsedTimeToPublish ms")
        return elapsedTimeToPublish
    }

    /**
     * Get a Local Date object from the given UTC time
     */
    fun getLocalDateFromUtcMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): Date {
        val utcTime = Date(timeMillisUtc)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return SimpleDateFormat(pattern, Locale.getDefault()).parse(sdf.format(utcTime))!!
    }

    /**
     * Get a Local Date String from the given UTC time
     */
    fun getLocalDateFormatFromUtcMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(getLocalDateFromUtcMillis(timeMillisUtc, pattern))
    }

    /**
     * Get a Local Date object from the given Local [TimeZone.getDefault()] time
     */
    private fun getLocalDateFromLocalMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): Date {
        val utcTime = Date(timeMillisUtc)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return SimpleDateFormat(pattern, Locale.getDefault()).parse(sdf.format(utcTime))!!
    }

    /**
     * Get a Local Date String from the given Local time
     */
    private fun getLocalDateFormatFromLocalMillis(timeMillisLocal: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(getLocalDateFromLocalMillis(timeMillisLocal, pattern))
    }

    fun getPostAtDateForListLayout(submission: Submission): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val timeDiff = submission.postAtMillis - cal.timeInMillis

        return when {
            (timeDiff < 86400000L) -> { // Draft is scheduled within 24 hrs
                "today ${getLocalDateFormatFromLocalMillis(submission.postAtMillis, "h:mm a")}"
            }
            (timeDiff in 86400000L..172799999) -> { // Draft is scheduled to post sometime tomorrow
                "tomorrow ${getLocalDateFormatFromLocalMillis(submission.postAtMillis, "h:mm a")}"
            }
            (timeDiff <= 604800000L) -> { // Draft is scheduled to post sometime this week
                getLocalDateFormatFromLocalMillis(submission.postAtMillis, "EEE h:mm a")
            }
            else -> {
                getLocalDateFormatFromLocalMillis(submission.postAtMillis, "MM/dd/yy h:mm a")
            }
        }
    }
}