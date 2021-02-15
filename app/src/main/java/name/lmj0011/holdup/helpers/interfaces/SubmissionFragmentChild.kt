package name.lmj0011.holdup.helpers.interfaces

import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.ui.submission.SubmissionViewModel

interface SubmissionFragmentChild {
    val viewModel:  SubmissionViewModel
    val submission: Submission?
    val actionBarTitle: String?

    /**
     * Call this in the onResume() method
     */
    fun updateActionBarTitle()
}