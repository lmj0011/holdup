package name.lmj0011.holdup.helpers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.kodein.di.instance
import timber.log.Timber

/**
 * This Worker attempts to post a comment to Reddit on behalf of a user until successful
 *
 * This Worker is intended to be "One Time, Deferrable"
 * ref: https://developer.android.com/topic/libraries/architecture/workmanager#types
 *
 */
class CommentDelayWorker (appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        /** Tags for WorkRequests based on this Worker  */
        const val GROUP_TAG = "name.lmj0011.holdup.helpers.workers.CommentDelayWorker"

        /** Key names for this Worker's InputData  */
        const val IN_KEY_ACCOUNT_ID = "IN_KEY_ACCOUNT_ID"
        const val IN_KEY_TEXT = "IN_KEY_TEXT"
        const val IN_KEY_THING_ID = "IN_KEY_THING_ID" // The fullname of the Reddit Thing that's being replied to

        /** Key names for this Worker's OutputData  */
        const val OUT_KEY_COMMENT_PERMALINK = "OUT_KEY_COMMENT_PERMALINK"
        const val OUT_KEY_HTTP_STATUS_CODE = "OUT_KEY_HTTP_STATUS_CODE"
        const val OUT_KEY_HTTP_STATUS_MSG = "OUT_KEY_HTTP_STATUS_MSG"
        const val OUT_KEY_ERROR_MSG = "OUT_KEY_ERROR_MSG"

        const val BACKOFF_DELAY_MINUTES = 10L
        const val MAXIMUM_WORKER_RETRIES = 5
    }

    private val dao = AppDatabase.getInstance(appContext).sharedDao
    private var redditAuthHelper: RedditAuthHelper = (appContext as App).kodein.instance()
    private val redditApiHelper: RedditApiHelper = (appContext as App).kodein.instance()
    lateinit var json: JSONObject
    lateinit var response: Response

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val acctId = inputData.getLong(IN_KEY_ACCOUNT_ID, 0L)
        val replyBody = inputData.getString(IN_KEY_TEXT)!!
        val thingId = inputData.getString(IN_KEY_THING_ID)!!

        val account = dao.getAccount(acctId)!!

        if(runAttemptCount > MAXIMUM_WORKER_RETRIES) {
            val errMsg = "MAXIMUM_WORKER_RETRIES ($MAXIMUM_WORKER_RETRIES) exceeded."
            val outputData = workDataOf(
                OUT_KEY_ERROR_MSG to errMsg
            )

            NotificationHelper.showCommentFailedToPublishNotification(errMsg, "", account)
            return@withContext Result.failure(outputData)
        }

        try {
            response = redditApiHelper.postComment(
                thingId,
                replyBody,
                redditAuthHelper.authClient(account).getSavedBearer().getAccessToken()!!
            )

            json = JSONObject(response.body!!.source().readUtf8())

            val commentThing = json
                .getJSONObject("json")
                .getJSONObject("data")
                .getJSONArray("things").getJSONObject(0)
                .getJSONObject("data")

            Timber.d("commentThing: $commentThing")

            val outputData = workDataOf(
                OUT_KEY_COMMENT_PERMALINK to commentThing.getString("permalink"),
                OUT_KEY_HTTP_STATUS_CODE to response.code,
                OUT_KEY_HTTP_STATUS_MSG to response.message,
            )

            NotificationHelper.showCommentPublishedNotification(
                commentThing.getString("permalink"),
                replyBody,
                account
            )

            return@withContext Result.success(outputData)
        } catch (ex: JSONException) {
            /**
             * If we have hit a Ratelimit, retry this Worker
             */
            val errors = json
                .getJSONObject("json")
                .getJSONArray("errors")

            if(ex.message == "No value for data"
                && errors.getJSONArray(0).getString(0) == "RATELIMIT") {

                Timber.i(errors.getJSONArray(0).getString(1)) // the actual RATELIMIT message

                val errorMsg = applicationContext.getString(R.string.reddit_api_ratelimit_hit_msg, CommentDelayWorker.BACKOFF_DELAY_MINUTES)

                NotificationHelper.showCommentRetryDueToRateLimitNotification(errorMsg, replyBody, account)
                return@withContext Result.retry()
            } else throw ex
        } catch (ex: HttpStatusException) {
            val errMsg = "${ex.javaClass.canonicalName} (${ex.statusCode})"

            val outputData = workDataOf(
                OUT_KEY_ERROR_MSG to errMsg
            )

            NotificationHelper.showCommentFailedToPublishNotification(errMsg, replyBody, account)
            Result.failure(outputData)
        } catch (ex: Exception) {
            Timber.e(ex)

            val outputData = workDataOf(
                OUT_KEY_ERROR_MSG to ex.javaClass.canonicalName
            )

            NotificationHelper.showCommentFailedToPublishNotification(ex.javaClass.canonicalName, replyBody, account)
            Result.failure(outputData)
        }

    }
}