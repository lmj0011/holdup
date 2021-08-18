package name.lmj0011.holdup.helpers.workers

import android.content.Context
import android.text.Html
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
import name.lmj0011.holdup.helpers.DateTimeHelper
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import org.jsoup.HttpStatusException
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

                Timber.d("Preparing to publish ${sub.kind?.name} Submission \"${sub.title}\"; scheduled time was ${DateTimeHelper.getPostAtDateForListLayout(sub)}")

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

                val responsePair = viewModel.populateFromSubmissionThenPost(sub, true)
                val errMsg =  responsePair?.second

                if (errMsg != null) {
                    val msg = Html.fromHtml(errMsg, Html.FROM_HTML_MODE_LEGACY)
                    NotificationHelper.showSubmissionPublishedErrorNotification(sub, msg)
                    val outputData = workDataOf("error" to msg)
                    return@withContext Result.failure(outputData)
                } else {
                    responsePair?.first?.let {
                        dao.deleteBySubmissionId(sub.id)
                    }
                }
            }
            return@withContext Result.success()
        } catch (ex: Exception) {
            submission?.let { sub ->
                val errMsg = if (ex is HttpStatusException) {
                    appContext.getString(R.string.scheduled_submission_publish_api_error_msg, sub.title, sub.subreddit?.displayNamePrefixed, ex.statusCode, ex.message)
                } else {
                    appContext.getString(R.string.scheduled_submission_publish_unknown_error_msg, sub.title, sub.subreddit?.displayNamePrefixed, ex.message)
                }

                val styledErrMsg = Html.fromHtml(errMsg, Html.FROM_HTML_MODE_LEGACY)
                NotificationHelper.showSubmissionPublishedErrorNotification(sub, styledErrMsg)
            }

            Timber.e(ex)
            return@withContext Result.failure(workDataOf("error" to ex.message))
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