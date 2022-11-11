package name.lmj0011.holdup.helpers

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.R
import name.lmj0011.holdup.ui.submission.EditSubmissionFragment
import timber.log.Timber

class FirebaseAnalyticsHelper(val context: Context) {
    val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    /**
     * [fragment] the fragment being used to generate a SCREEN_VIEW event
     */
    fun logScreenView(fragment: Fragment) {
        when(fragment) {
            is EditSubmissionFragment -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, "Edit Submission")
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "EditSubmissionFragment")
                }
            }
            else -> {
                Timber.d("Boooo! Unrecognized fragment")
            }
        }
    }

    /**
     * A more specialized version of [logScreenView] method; It's for logging screen views that
     * are triggered by user selection from a TabLayout
     *
     * [position] the tab layout position for a fragment; see [name.lmj0011.holdup.Keys]
     */
    fun logScreenViewByTabPosition(position: Int) {
        when(position) {
            Keys.LINK_TAB_POSITION -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, context.getString(R.string.link_submission_action_bar_title))
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "SubmissionFragment")
                }
            }
            Keys.IMAGE_TAB_POSITION -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, context.getString(R.string.image_submission_action_bar_title))
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "SubmissionFragment")
                }
            }
            Keys.VIDEO_TAB_POSITION -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, context.getString(R.string.video_submission_action_bar_title))
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "SubmissionFragment")
                }
            }
            Keys.SELF_TAB_POSITION -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, context.getString(R.string.self_submission_action_bar_title))
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "SubmissionFragment")
                }
            }
            Keys.POLL_TAB_POSITION -> {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, context.getString(R.string.poll_submission_action_bar_title))
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "SubmissionFragment")
                }
            }
            else -> {
                Timber.d("Boooo! Unrecognized Tab Position")
            }
        }
    }

    fun logDropMenuItemSelectedEvent(title: String) {
        firebaseAnalytics.logEvent("hol_dropmenu_item_selected") {
            param("menu_item", title)
        }
    }

    /**
     * [cnt] the total number Reddit accounts this instance has
     */
    fun logAccountAddedEvent(cnt: Int) {
        firebaseAnalytics.logEvent("hol_account_added") {
            param("total_accounts", cnt.toString())
        }
    }

    /**
     * [sr] the subreddit name
     * [kind] the type of Post
     */
    fun logPostScheduledEvent(sr: String, kind: String) {
        firebaseAnalytics.logEvent("hol_post_scheduled") {
            param("sr", sr)
            param("post_type", kind)
        }
    }

    /**
     * [sr] the subreddit name
     * [kind] the type of Post
     */
    fun logPostSuccessfulEvent(sr: String, kind: String) {
        firebaseAnalytics.logEvent("hol_post_successful") {
            param("sr", sr)
            param("post_type", kind)
        }
    }

    /**
     * [sr] the subreddit name
     * [kind] the type of Post
     * [errorMsg] the error message
     */
    fun logPostFailedEvent(sr: String, kind: String, errorMsg: String) {
        firebaseAnalytics.logEvent("hol_post_failed") {
            param("sr", sr)
            param("post_type", kind)
            param("error_msg", errorMsg)
        }
    }

    /**
     * [isPreSelected] whether the scheduled time was a preset option
     * [isPostNow] whether the user choose to submit Post now
     * [timeInMillis] the schedule time as a UTC millis timestamp
     */
    fun logScheduledDateTimeSelectedEvent(isPreSelected: Boolean, isPostNow: Boolean, timeInMillis: Long) {
        firebaseAnalytics.logEvent("hol_scheduled_date_time_selected") {
            param("isPreSelected", isPreSelected.toString())
            param("isPostNow", isPostNow.toString())
            param("timeInMillis", timeInMillis.toString())
        }
    }
}
