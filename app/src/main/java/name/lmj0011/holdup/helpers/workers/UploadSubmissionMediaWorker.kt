package name.lmj0011.holdup.helpers.workers

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.helpers.NotificationHelper
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.ui.submission.SubmissionViewModel
import org.kodein.di.instance
import timber.log.Timber
import java.util.*

/**
 * This Worker publishes a scheduled Submission to Reddit
 */
class UploadSubmissionMediaWorker (private val appContext: Context, private val parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    companion object {
        const val Progress = "Progress"
    }

    private val dao = AppDatabase.getInstance(appContext.applicationContext as App).sharedDao
    private val viewModel: SubmissionViewModel = SubmissionViewModel.getNewInstance(
        AppDatabase.getInstance(appContext.applicationContext as App).sharedDao,
        appContext.applicationContext as App
    )
    private val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    var progressUpdateData = workDataOf(Progress to 0)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val submissions = dao.getAllSubmissions()
            .filter { sub -> // Submission is posting within 1 hour
                val truth = ((sub.postAtMillis - cal.timeInMillis) <= 3600000L)
                Timber.d("${sub.kind?.name} submission title: ${sub.title}")
                Timber.d("equation is $truth: ${sub.postAtMillis} - ${cal.timeInMillis} = ${(sub.postAtMillis - cal.timeInMillis)}")
                truth
            }
            .filter { sub -> // Submissions with attached media that have not been uploaded
                when(sub.kind) {
                    SubmissionKind.Image -> {
                        sub.imgGallery.any { img ->
                            img.url.isBlank() || img.mediaId.isBlank()
                        }
                    }
                    SubmissionKind.Video, SubmissionKind.VideoGif -> {
                        Timber.d("sub.video: ${sub.video}")
                        sub.video != null || sub.video!!.url.isBlank() || sub.video!!.mediaId.isBlank()
                    }
                    else -> false
                }
            }

        if(submissions.isEmpty()) return@withContext Result.success()

        val redditAuthHelper: RedditAuthHelper = (appContext.applicationContext as App).kodein.instance()
        val redditApiHelper: RedditApiHelper = (appContext.applicationContext as App).kodein.instance()

        setForeground(createForegroundInfo())
        delay(3000L) // give Foreground Notif time to show?
        setProgress(progressUpdateData)

        try {
            var progressStep: Int

            submissions
                .also { list ->
                    var mediaCnt = 0

                    list.forEach { sub ->
                        when(sub.kind) {
                            SubmissionKind.Image -> {
                                mediaCnt += sub.imgGallery.size
                            }
                            SubmissionKind.Video, SubmissionKind.VideoGif -> {
                                mediaCnt++
                            }
                            else -> {}
                        }
                    }

                    progressStep = if (mediaCnt == 0) 100 else (100 / mediaCnt)
                    Timber.d("list.size: ${list.size}")
                    Timber.d("progressStep: $progressStep")
                }
                .forEach { sub ->
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
                    viewModel.populateFromSubmissionThenPost(sub, false) // setting the Livedata in this viewmodel
                    delay(2000L) // give time for Livedata to settle?
                    /**
                     *
                     */

                    when(sub.kind) {
                        SubmissionKind.Image ->  {
                            sub.apply {
                                val list = mutableListOf<Image>()

                                imgGallery.forEach { img ->
                                    val mediaInfo = redditApiHelper.uploadMedia(
                                        Uri.parse(img.sourceUri),
                                        redditAuthHelper.authClient(sub.account).getSavedBearer().getAccessToken()!!
                                    )
                                    img.mediaId = mediaInfo.first
                                    img.url = mediaInfo.second
                                    list.add(img)

                                    addToProgress(progressStep)
                                    Timber.d("Uploaded media for ${kind?.name} Submission \"$title\"; ${img.url}")
                                }

                                imgGallery = list
                            }
                            AppDatabase.getInstance(appContext.applicationContext as App).sharedDao.update(sub)
                        }
                        SubmissionKind.Video, SubmissionKind.VideoGif -> {
                            val video = sub.video!!

                            sub.apply {
                                Timber.d("video before: ${this.video}")

                                val isVideoUploaded = video.url.take(8) == "https://"
                                Timber.d("isVideoUploaded: $isVideoUploaded")

                                if(!isVideoUploaded) {
                                    val videoMediaInfo = redditApiHelper.uploadMedia(
                                        Uri.parse(video.sourceUri),
                                        redditAuthHelper.authClient(sub.account).getSavedBearer().getAccessToken()!!
                                    )
                                    Timber.d("videoMediaInfo: $videoMediaInfo")


                                    video.mediaId = videoMediaInfo.first
                                    video.url = videoMediaInfo.second
                                }


                                val isVideoThumbNailUploaded = video.posterUrl.take(8) == "https://"
                                Timber.d("isVideoThumbNailUploaded: $isVideoThumbNailUploaded")

                                if (!isVideoThumbNailUploaded) {
                                    val videoPosterMediaInfo = redditApiHelper.uploadMedia(
                                        Uri.parse(video.posterUrl),
                                        redditAuthHelper.authClient(sub.account).getSavedBearer().getAccessToken()!!
                                    )
                                    Timber.d("videoPosterMediaInfo: $videoPosterMediaInfo")

                                    video.posterUrl = videoPosterMediaInfo.second
                                }

                                this.video = video

                                Timber.d("video after: ${this.video}")
                                addToProgress(progressStep)
                                Timber.d("Uploaded media for ${kind?.name} Submission \"$title\"; ${video.url}")
                            }
                            AppDatabase.getInstance(appContext.applicationContext as App).sharedDao.update(sub)
                        }
                        else -> {}
                    }

                }

            return@withContext Result.success()
        } catch (ex: Exception) {
            Timber.e(ex)
            return@withContext Result.failure()
        }
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount > 3) {
            return Result.failure()
        } else Result.retry()
    }

    // https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running#foreground-service-type
    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHelper.getUploadingSubmissionMediaForegroundServiceNotification()
        return ForegroundInfo(NotificationHelper.UPLOADING_SUBMISSION_MEDIA_NOTIFICATION_ID, notification)
    }

    private suspend fun addToProgress(step: Int) {
        val progress = progressUpdateData.getInt(Progress, 0) + step
        progressUpdateData = workDataOf(Progress to progress)
        showProgress(progress)
        setProgress(progressUpdateData)
    }

    private fun showProgress(progress: Int) {
        val notification = NotificationHelper
            .getUploadingSubmissionMediaForegroundServiceNotification(progress)

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.UPLOADING_SUBMISSION_MEDIA_NOTIFICATION_ID, notification)
        }
    }
}