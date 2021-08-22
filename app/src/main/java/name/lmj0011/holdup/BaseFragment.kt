package name.lmj0011.holdup

import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import name.lmj0011.holdup.helpers.workers.PublishScheduledSubmissionWorker
import name.lmj0011.holdup.helpers.workers.UploadSubmissionMediaWorker
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class BaseFragment(contentLayoutId: Int): Fragment(contentLayoutId) {
    protected fun pickDateAndTime(callBack: (cal: Calendar) -> Unit) {
        // see https://github.com/material-components/material-components-android/issues/882#issuecomment-638983598
        val cal = Calendar.getInstance()
        // ^ because the datepicker give us UTC time to convert ourselves

        // big Up! https://stackoverflow.com/a/62080582/2445763
        val constraintBuilder = CalendarConstraints.Builder()
        constraintBuilder.setValidator(DateValidatorPointForward.now())

        val dateBuilder = MaterialDatePicker.Builder.datePicker()
        dateBuilder.setCalendarConstraints(constraintBuilder.build())
        val datePicker = dateBuilder.build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            val dummyCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            dummyCal.timeInMillis = millis

            cal.set(Calendar.YEAR, dummyCal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, dummyCal.get(Calendar.MONTH))
            cal.set(Calendar.DAY_OF_MONTH, dummyCal.get(Calendar.DAY_OF_MONTH))

            askForTime(cal, callBack)
        }

        datePicker.show(parentFragmentManager, "date_picker_tag")

    }

    private fun askForTime(cal: Calendar, callback: (cal: Calendar) -> Unit) {
        @TimeFormat val clockFormat = when(DateFormat.is24HourFormat(context)) {
            true -> TimeFormat.CLOCK_24H
            else -> TimeFormat.CLOCK_12H
        }

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            cal.set(Calendar.MINUTE, timePicker.minute)
            callback(cal)
        }

        timePicker.show(parentFragmentManager, "time_picker_tag")
    }

    protected fun enqueueUploadSubmissionMediaWorkerThenPublish(alarmRequestCode: Int) {
        val workManager = WorkManager.getInstance(requireContext())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadSubmissionMediaWorkRequest = OneTimeWorkRequestBuilder<UploadSubmissionMediaWorker>()
            .setConstraints(constraints)
            .build()

        val publishScheduledSubmissionWorkerData = Data.Builder()
            .putInt("alarmRequestCode", alarmRequestCode)
            .build()

        val publishScheduledSubmissionWorkRequest = OneTimeWorkRequestBuilder<PublishScheduledSubmissionWorker>()
            .setConstraints(constraints)
            .setInputData(publishScheduledSubmissionWorkerData)
            .build()

        workManager
            .beginUniqueWork(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG, ExistingWorkPolicy.REPLACE, uploadSubmissionMediaWorkRequest)
            .then(publishScheduledSubmissionWorkRequest)
            .enqueue()
    }

    protected fun enqueueUploadSubmissionMediaWorker() {
        val workManager = WorkManager.getInstance(requireContext())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadSubmissionMediaWorkRequest = OneTimeWorkRequestBuilder<UploadSubmissionMediaWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        workManager
            .enqueueUniqueWork(Keys.UPLOAD_SUBMISSION_MEDIA_WORKER_TAG, ExistingWorkPolicy.REPLACE, uploadSubmissionMediaWorkRequest)
    }
}