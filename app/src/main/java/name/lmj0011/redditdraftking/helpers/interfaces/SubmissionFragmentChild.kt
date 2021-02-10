package name.lmj0011.redditdraftking.helpers.interfaces

import name.lmj0011.redditdraftking.database.models.Submission
import name.lmj0011.redditdraftking.ui.submission.SubmissionViewModel

interface SubmissionFragmentChild {
    val viewModel:  SubmissionViewModel
    val submission: Submission?
    val actionBarTitle: String?

    /**
     * Call this in the onResume() method
     */
    fun updateActionBarTitle()
}