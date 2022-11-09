package name.lmj0011.holdup

import name.lmj0011.holdup.helpers.DateTimeHelper
import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 *
 * NOTE: If you are wanting to display output in the console do: System.out.println("Test")
 */
class DateTimeHelperUnitTest {

    @Test
    fun getPreScheduledPostTimes_isCorrect() {
        val calendar1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val startTime = 1736899200000L //  January 15, 2025 00:00:00 (UTC)
        val incrementBy1Min = 60000L // 1 min in ms
        val incrementBy1Hour = 3600000L // 1 hour in ms
        val incrementBy1Day = 86400000L // 1 day in ms

        /**
         * This for loop starts at January 15, 2025 00:00:00 (UTC) and
         * ends at January 15, 2025 07:59:00 (UTC)
         */
        for (millis in startTime until(startTime + incrementBy1Hour * 8) step incrementBy1Min) {
            calendar1.timeInMillis = millis
            calendar2.timeInMillis = millis
            val preSetTimeMap = DateTimeHelper.getPreScheduledPostTimes(calendar1, calendar2)

            assertEquals(true, preSetTimeMap.containsKey("option1"))
            assertEquals(true, preSetTimeMap.containsKey("option2"))
            assertEquals(true, preSetTimeMap.containsKey("option3"))

            assertEquals("Morning", preSetTimeMap["option1"]!!.first)
            assertEquals("Afternoon", preSetTimeMap["option2"]!!.first)
            assertEquals("Evening", preSetTimeMap["option3"]!!.first)

            assertEquals("8:00 AM", preSetTimeMap["option1"]!!.second)
            assertEquals("1:00 PM", preSetTimeMap["option2"]!!.second)
            assertEquals("6:00 PM", preSetTimeMap["option3"]!!.second)

            assertEquals(startTime + incrementBy1Hour * 8, preSetTimeMap["option1"]!!.third)
            assertEquals(startTime + incrementBy1Hour * 13, preSetTimeMap["option2"]!!.third)
            assertEquals(startTime + incrementBy1Hour * 18, preSetTimeMap["option3"]!!.third)
        }

        /**
         * This for loop starts at January 15, 2025 08:00:00 (UTC) and
         * ends at January 15, 2025 12:59:00 (UTC)
         */
        for (millis in (startTime + incrementBy1Hour * 8) until(startTime + incrementBy1Hour * 13) step incrementBy1Min) {
            calendar1.timeInMillis = millis
            calendar2.timeInMillis = millis
            val preSetTimeMap = DateTimeHelper.getPreScheduledPostTimes(calendar1, calendar2)

            assertEquals(true, preSetTimeMap.containsKey("option1"))
            assertEquals(true, preSetTimeMap.containsKey("option2"))
            assertEquals(true, preSetTimeMap.containsKey("option3"))

            assertEquals("Afternoon", preSetTimeMap["option1"]!!.first)
            assertEquals("Evening", preSetTimeMap["option2"]!!.first)
            assertEquals("Tonight", preSetTimeMap["option3"]!!.first)

            assertEquals("1:00 PM", preSetTimeMap["option1"]!!.second)
            assertEquals("6:00 PM", preSetTimeMap["option2"]!!.second)
            assertEquals("9:00 PM", preSetTimeMap["option3"]!!.second)

            assertEquals(startTime + incrementBy1Hour * 13, preSetTimeMap["option1"]!!.third)
            assertEquals(startTime + incrementBy1Hour * 18, preSetTimeMap["option2"]!!.third)
            assertEquals(startTime + incrementBy1Hour * 21, preSetTimeMap["option3"]!!.third)
        }

        /**
         * This for loop starts at January 15, 2025 13:00:00 (UTC) and
         * ends at January 15, 2025 17:59:00 (UTC)
         */
        for (millis in (startTime + incrementBy1Hour * 13) until(startTime + incrementBy1Hour * 18) step incrementBy1Min) {
            calendar1.timeInMillis = millis
            calendar2.timeInMillis = millis
            val preSetTimeMap = DateTimeHelper.getPreScheduledPostTimes(calendar1, calendar2)

            assertEquals(true, preSetTimeMap.containsKey("option1"))
            assertEquals(true, preSetTimeMap.containsKey("option2"))
            assertEquals(true, preSetTimeMap.containsKey("option3"))

            assertEquals("Evening", preSetTimeMap["option1"]!!.first)
            assertEquals("Tonight", preSetTimeMap["option2"]!!.first)
            assertEquals("Tomorrow morning", preSetTimeMap["option3"]!!.first)

            assertEquals("6:00 PM", preSetTimeMap["option1"]!!.second)
            assertEquals("9:00 PM", preSetTimeMap["option2"]!!.second)
            assertEquals("8:00 AM", preSetTimeMap["option3"]!!.second)

            assertEquals(startTime + incrementBy1Hour * 18, preSetTimeMap["option1"]!!.third)
            assertEquals(startTime + incrementBy1Hour * 21, preSetTimeMap["option2"]!!.third)
            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 8, preSetTimeMap["option3"]!!.third)
        }

        /**
         * This for loop starts at January 15, 2025 18:00:00 (UTC) and
         * ends at January 15, 2025 20:59:00 (UTC)
         */
        for (millis in (startTime + incrementBy1Hour * 18) until (startTime + incrementBy1Hour * 21) step incrementBy1Min) {
            calendar1.timeInMillis = millis
            calendar2.timeInMillis = millis
            val preSetTimeMap = DateTimeHelper.getPreScheduledPostTimes(calendar1, calendar2)

            assertEquals(true, preSetTimeMap.containsKey("option1"))
            assertEquals(true, preSetTimeMap.containsKey("option2"))
            assertEquals(true, preSetTimeMap.containsKey("option3"))

            assertEquals("Tonight", preSetTimeMap["option1"]!!.first)
            assertEquals("Tomorrow morning", preSetTimeMap["option2"]!!.first)
            assertEquals("Tomorrow afternoon", preSetTimeMap["option3"]!!.first)

            assertEquals("9:00 PM", preSetTimeMap["option1"]!!.second)
            assertEquals("8:00 AM", preSetTimeMap["option2"]!!.second)
            assertEquals("1:00 PM", preSetTimeMap["option3"]!!.second)

            assertEquals(startTime + incrementBy1Hour * 21, preSetTimeMap["option1"]!!.third)
            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 8, preSetTimeMap["option2"]!!.third)
            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 13, preSetTimeMap["option3"]!!.third)
        }

        /**
         * This for loop starts at January 15, 2025 21:00:00 (UTC) and
         * ends at January 15, 2025 23:59:00 (UTC)
         */
        for (millis in (startTime + incrementBy1Hour * 21) until (startTime + incrementBy1Hour * 24) step incrementBy1Min) {
            calendar1.timeInMillis = millis
            calendar2.timeInMillis = millis
            val preSetTimeMap = DateTimeHelper.getPreScheduledPostTimes(calendar1, calendar2)

            assertEquals(true, preSetTimeMap.containsKey("option1"))
            assertEquals(true, preSetTimeMap.containsKey("option2"))
            assertEquals(true, preSetTimeMap.containsKey("option3"))

            assertEquals("Tomorrow morning", preSetTimeMap["option1"]!!.first)
            assertEquals("Tomorrow afternoon", preSetTimeMap["option2"]!!.first)
            assertEquals("Tomorrow evening", preSetTimeMap["option3"]!!.first)

            assertEquals("8:00 AM", preSetTimeMap["option1"]!!.second)
            assertEquals("1:00 PM", preSetTimeMap["option2"]!!.second)
            assertEquals("6:00 PM", preSetTimeMap["option3"]!!.second)

            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 8, preSetTimeMap["option1"]!!.third)
            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 13, preSetTimeMap["option2"]!!.third)
            assertEquals(startTime + incrementBy1Day + incrementBy1Hour * 18, preSetTimeMap["option3"]!!.third)
        }
    }
}