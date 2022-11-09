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

//    /**
//     * Get a Local Date object from the given UTC time
//     */
//    private fun convertUtcMillisToUtcDate(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): Date {
//        val utcTime = Date(timeMillisUtc)
//        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
//        sdf.timeZone = TimeZone.getTimeZone("UTC")
//        return SimpleDateFormat(pattern, Locale.getDefault()).parse(sdf.format(utcTime))!!
//    }
//
//    /**
//     * Get a Local Date String from the given UTC time
//     */
//    fun getUtcDateFormatFromUtcMillis(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
//        return SimpleDateFormat(pattern, Locale.getDefault()).format(convertUtcMillisToUtcDate(timeMillisUtc, pattern))
//    }

    /**
     * Get a Local Date object from the given Local [TimeZone.getDefault()] time
     */
    private fun convertUtcMillisToLocalDate(timeMillisUtc: Long, pattern: String = "yyyy/MM/dd HH:mm"): Date {
        val utcTime = Date(timeMillisUtc)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return SimpleDateFormat(pattern, Locale.getDefault()).parse(sdf.format(utcTime))!!
    }

    /**
     * Get a Local Date String from the given Local time
     */
    private fun getLocalDateFormatFromUtcMillis(timeMillisLocal: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(convertUtcMillisToLocalDate(timeMillisLocal, pattern))
    }

    fun getPreScheduledPostTimes(currentCal: Calendar, futureCal: Calendar): Map<String, Triple<String, String, Long>> {
        futureCal.set(Calendar.MINUTE, 0)
        futureCal.set(Calendar.SECOND, 0)

        futureCal.set(Calendar.HOUR_OF_DAY, 8)
        val todayMorning0800Millis = futureCal.timeInMillis

        futureCal.set(Calendar.HOUR_OF_DAY, 13)
        val todayAfternoon1300Millis = futureCal.timeInMillis

        futureCal.set(Calendar.HOUR_OF_DAY, 18)
        val todayEvening1800Millis = futureCal.timeInMillis

        futureCal.set(Calendar.HOUR_OF_DAY, 21)
        val tonight2100Millis = futureCal.timeInMillis

        futureCal.add(Calendar.DAY_OF_YEAR, 1) // add a day

        futureCal.set(Calendar.HOUR_OF_DAY, 8)
        val tomorrowMorning0800Millis = futureCal.timeInMillis

        futureCal.set(Calendar.HOUR_OF_DAY, 13)
        val tomorrowAfternoon1300Millis = futureCal.timeInMillis

        futureCal.set(Calendar.HOUR_OF_DAY, 18)
        val tomorrowEvening1800Millis = futureCal.timeInMillis



        val after0000State = mapOf(
            "option1" to Triple("Morning", "8:00 AM", todayMorning0800Millis),
            "option2" to Triple("Afternoon", "1:00 PM", todayAfternoon1300Millis),
            "option3" to Triple("Evening", "6:00 PM", todayEvening1800Millis)
        )

        val after0800State = mapOf(
            "option1" to Triple("Afternoon", "1:00 PM", todayAfternoon1300Millis),
            "option2" to Triple("Evening", "6:00 PM", todayEvening1800Millis),
            "option3" to Triple("Tonight", "9:00 PM", tonight2100Millis)
        )

        val after1300State = mapOf(
            "option1" to Triple("Evening", "6:00 PM", todayEvening1800Millis),
            "option2" to Triple("Tonight", "9:00 PM", tonight2100Millis),
            "option3" to Triple("Tomorrow morning", "8:00 AM", tomorrowMorning0800Millis)
        )

        val after1800State = mapOf(
            "option1" to Triple("Tonight", "9:00 PM", tonight2100Millis),
            "option2" to Triple("Tomorrow morning", "8:00 AM", tomorrowMorning0800Millis),
            "option3" to Triple("Tomorrow afternoon", "1:00 PM", tomorrowAfternoon1300Millis)
        )

        val after2100State = mapOf(
            "option1" to Triple("Tomorrow morning", "8:00 AM", tomorrowMorning0800Millis),
            "option2" to Triple("Tomorrow afternoon", "1:00 PM", tomorrowAfternoon1300Millis),
            "option3" to Triple("Tomorrow evening", "6:00 PM", tomorrowEvening1800Millis)
        )

        return when {
            (currentCal.timeInMillis < todayMorning0800Millis) -> after0000State
            (currentCal.timeInMillis in todayMorning0800Millis until todayAfternoon1300Millis) -> after0800State
            (currentCal.timeInMillis in todayAfternoon1300Millis until todayEvening1800Millis) -> after1300State
            (currentCal.timeInMillis in todayEvening1800Millis until tonight2100Millis) -> after1800State
            (currentCal.timeInMillis >= tonight2100Millis) -> after2100State
            else -> throw Error("Unexpected time.")

        }
    }

    fun getPostAtDateForListLayout(submission: Submission): String {
        val currentCal = Calendar.getInstance() // uses local timezone by default
        val postAtCal = Calendar.getInstance()
        postAtCal.timeInMillis =  submission.postAtMillis


        Timber.d("postAtCal.timeInMillis: ${postAtCal.timeInMillis}")

        currentCal.add(Calendar.DAY_OF_YEAR, 1)
        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        val tomorrowStartMillis = currentCal.timeInMillis

        Timber.d("tomorrowStartMillis: $tomorrowStartMillis")

        currentCal.add(Calendar.DAY_OF_YEAR, 1)
        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        val tomorrowEndMillis = currentCal.timeInMillis

        Timber.d("tomorrowEndMillis: $tomorrowEndMillis")

        /**
         * adding a week (7 days) here, but only needed to add 5 since
         * we already advanced 2 days on previous calls to currentCal.add
         */
        currentCal.add(Calendar.DAY_OF_YEAR, 5)
        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        val endOfFollowingWeekMillis = currentCal.timeInMillis

        Timber.d("endOfFollowingWeekMillis: $endOfFollowingWeekMillis")

        return when {
            (postAtCal.timeInMillis < tomorrowStartMillis) -> { // Post is scheduled for today
                "today ${getLocalDateFormatFromUtcMillis(submission.postAtMillis, "h:mm a")}"
            }
            (postAtCal.timeInMillis in tomorrowStartMillis..tomorrowEndMillis) -> { // Post is scheduled for some time tomorrow
                "tomorrow ${getLocalDateFormatFromUtcMillis(submission.postAtMillis, "h:mm a")}"
            }
            (postAtCal.timeInMillis in tomorrowEndMillis..endOfFollowingWeekMillis) -> { // Post is scheduled within a week from postAtCal.timeInMillis
                getLocalDateFormatFromUtcMillis(submission.postAtMillis, "EEE h:mm a")
            }
            else -> { // Post is scheduled over a week in advanced
                getLocalDateFormatFromUtcMillis(submission.postAtMillis, "MM/dd/yy h:mm a")
            }
        }
    }
}