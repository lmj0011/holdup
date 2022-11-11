package name.lmj0011.holdup.helpers.interfaces

import com.google.firebase.analytics.FirebaseAnalytics
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.helpers.FirebaseAnalyticsHelper
import name.lmj0011.holdup.ui.submission.SubmissionViewModel

interface SubmissionFragmentChildInterface {
    companion object {
     const val VIEW_MODE = 0
     const val CREATE_AND_EDIT_MODE = 1 // the default
    }

    val firebaseAnalyticsHelper: FirebaseAnalyticsHelper
    val viewModel:  SubmissionViewModel
    val submission: Submission?
    val mode: Int

    /**
     * Call this in the onResume() method
     */
    fun updateActionBarTitle()
}