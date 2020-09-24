package name.lmj0011.redditdraftking.helpers.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.Draft
import name.lmj0011.redditdraftking.helpers.NotificationHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.enums.DraftKind
import name.lmj0011.redditdraftking.helpers.enums.SubmitKind
import name.lmj0011.redditdraftking.helpers.receivers.PublishScheduledDraftReceiver
import name.lmj0011.redditdraftking.helpers.services.ScheduledDraftForegroundService
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

/**
 * This Worker publishes a drafted post to reddit
 */
class PublishScheduledDraftWorker (private val appContext: Context, private val parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    private val alarmMgr = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dao = AppDatabase.getInstance(appContext.applicationContext as App).sharedDao
    private val redditAuthHelper: RedditAuthHelper = (appContext.applicationContext as App).kodein.instance()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val isAuthorized =  redditAuthHelper.isAuthorized.first()
        Timber.d("redditAuthHelper.isAuthorized: $isAuthorized")

        val draftUuid = parameters.inputData.getString("draftUuid")

        if (isAuthorized && !draftUuid.isNullOrBlank()) {
            val draft = dao.getDraft(draftUuid)
            Timber.d("draft: $draft")

            draft?.let {d ->
                //refresh
                redditAuthHelper.authClient.getSavedBearer().renewToken()

                // post draft
                val isSuccess = postDraft(d)
                Timber.d("postDraft() was Successful? $isSuccess")

                // delete alarm
                val oldAlarmIntent = Intent(appContext, PublishScheduledDraftReceiver::class.java).let { intent ->
                    intent.putExtra("draftUuid", d.uuid)
                    PendingIntent.getBroadcast(appContext, d.requestCode, intent, 0)
                }

                alarmMgr.cancel(oldAlarmIntent)

                // delete draft upon successful post
                dao.deleteByDraftId(d.uuid)

                WorkManager.getInstance(appContext).enqueue(OneTimeWorkRequestBuilder<ScheduledDraftServiceCallerWorker>().build())
                return@withContext Result.success()
            }
            return@withContext retryOrFail()
        } else retryOrFail()
    }

    private fun retryOrFail(): Result {
        WorkManager.getInstance(appContext).enqueue(OneTimeWorkRequestBuilder<ScheduledDraftServiceCallerWorker>().build())

        return if (runAttemptCount > 3) {
            return Result.failure()
        } else Result.retry()
    }

    private fun postDraft(draft: Draft): Boolean {
        val client = OkHttpClient()
        val subreddit = dao.getSubreddit(draft.subredditUuid)!!

        val formBodyBuilder = FormBody.Builder()
            .add("sr", subreddit.displayName)
            .add("api_type", "json")
            .add("draft_id", draft.uuid)
            .add("title", draft.title)

        when(DraftKind.from(draft.kind)) {
            DraftKind.MarkDown -> {
                formBodyBuilder
                    .add("kind", SubmitKind.Self.kind)
                    .add("text", draft.body)
            }
            DraftKind.RichText -> {
                Timber.d("JSONObject(draft.body).toString(): ${JSONObject(draft.body).toString()}")
                formBodyBuilder
                    .add("kind", SubmitKind.Self.kind)
                    .add("richtext_json", JSONObject(draft.body).toString())
            }
            DraftKind.Link -> {
                formBodyBuilder
                    .add("kind", SubmitKind.Link.kind)
                    .add("url", draft.body)
            }
        }

        val request = Request.Builder()
            .url("https://oauth.reddit.com/api/submit?resubmit=true")
            .header("Authorization", "Bearer ${redditAuthHelper.authClient.getSavedBearer().getAccessToken()}")
            .post(formBodyBuilder.build())
            .build()

        val response = client.newCall(request).execute()


        // show notification of successful post
        if(response.isSuccessful) {
            try {
                val json = JSONObject(response.body!!.string())
                val url = json.getJSONObject("json").getJSONObject("data").getString("url")
                NotificationHelper.showDraftPublishedNotification(Pair(draft, subreddit), url)
            } catch(ex: Exception) {
                Timber.e(ex)
            }
        }

        return response.isSuccessful
    }
}