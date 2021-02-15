package name.lmj0011.holdup.helpers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import timber.log.Timber
import java.util.*

/**
 * This Worker publishes a scheduled Submission to Reddit
 *
 * TODO - need some way to let the user know if the Submission failed to post
 */
class PublishScheduledSubmissionWorker (private val appContext: Context, private val parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    private val dao = AppDatabase.getInstance(appContext.applicationContext as App).sharedDao
    private val viewModel: SubmissionViewModel = SubmissionViewModel.getNewInstance(
        AppDatabase.getInstance(appContext.applicationContext as App).sharedDao,
        appContext.applicationContext as App
    )
    private val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val alarmRequestCode = parameters.inputData.getInt("alarmRequestCode", -1)
        val submission = dao.getSubmissionByAlarmRequestCode(alarmRequestCode)

        try {
            submission?.let { sub ->
                if(submission.postAtMillis > cal.timeInMillis) {
                    val outputData = workDataOf("warning" to appContext.getString(R.string.too_early_submission))
                    return@withContext Result.failure(outputData)
                }

                setForeground(createForegroundInfo(sub))

                /**
                 * This block of code is a dumb hack to prevent from having to do a rewrite
                 * in order to make use of a CoroutineExceptionHandler. This will probably
                 * come back as tech debt in the future.
                 *
                 * We are trying to avoid this error:
                 * java.lang.NullPointerException @ SubmissionViewModel$postSubmission$1.invokeSuspend(SubmissionViewModel.kt:282)
                 *
                 * ref: https://www.lukaslechner.com/why-exception-handling-with-kotlin-coroutines-is-so-hard-and-how-to-successfully-master-it/
                 */
                delay(3000L) // give Foreground Notif time to show?
                viewModel.populateFromSubmissionThenPost(sub, false) // setting the Livedata in this viewmodel
                delay(2000L) // give time for Livedata to settle?
                /**
                 *
                 */

                val responsePair = viewModel.populateFromSubmissionThenPost(sub)

                responsePair?.first?.let {
                    dao.deleteBySubmissionId(sub.id)
                }

                responsePair?.second?.let { msg ->
                    NotificationHelper.showSubmissionPublishedErrorNotification(sub, msg)
                    val outputData = workDataOf("error" to msg)
                    return@withContext Result.failure(outputData)
                }
            }

            return@withContext Result.success()
        } catch (ex: Exception) {
            val outputData = workDataOf("error" to ex.message)
            Timber.e(ex)
            return@withContext Result.failure(outputData)
        }
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount > 3) {
            return Result.failure()
        } else Result.retry()
    }

    // https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running#foreground-service-type
    private fun createForegroundInfo(submission: Submission): ForegroundInfo {
        val notification = NotificationHelper.getPostingSubmissionForegroundServiceNotification(submission)
        return ForegroundInfo(NotificationHelper.POSTING_SCHEDULED_SUBMISSION_SERVICE_NOTIFICATION_ID, notification)
    }
}