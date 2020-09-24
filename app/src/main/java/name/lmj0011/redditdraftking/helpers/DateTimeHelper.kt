package name.lmj0011.redditdraftking.helpers

import name.lmj0011.redditdraftking.database.Draft
import java.text.SimpleDateFormat
import java.util.*

object DateTimeHelper {

    /**
     * Get a Local Date object from the given UTC time
     */
    fun getLocalDateFromUtcMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): Date? {
        val utcTime = Date(timeMillisUtc)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return SimpleDateFormat(pattern, Locale.getDefault()).parse(sdf.format(utcTime))
    }

    /**
     * Get a Local Date String from the given UTC time
     */
    fun getLocalDateFormatFromUtcMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(getLocalDateFromUtcMillis(timeMillisUtc, pattern)!!)
    }

    fun getPostAtDateForListLayout(draft: Draft): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val timeDiff = draft.postAtMillis - cal.timeInMillis

        return when {
            (timeDiff < 86400000L) -> { // Draft is scheduled within 24 hrs
                "today ${getLocalDateFormatFromUtcMillis(draft.postAtMillis, "h:mm a")}"
            }
            (timeDiff in 86400000L..172799999) -> { // Draft is scheduled to post sometime tomorrow
                "tomorrow ${getLocalDateFormatFromUtcMillis(draft.postAtMillis, "h:mm a")}"
            }
            (timeDiff <= 604800000L) -> { // Draft is scheduled to post sometime this week
                getLocalDateFormatFromUtcMillis(draft.postAtMillis, "EEE h:mm a")
            }
            else -> {
                getLocalDateFormatFromUtcMillis(draft.postAtMillis, "MM/dd/yy h:mm a")
            }
        }
    }
}