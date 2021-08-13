package name.lmj0011.holdup.helpers.interfaces

import android.content.Context
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.ui.submission.SubmissionViewModel

interface SubmissionFragmentChild {
    companion object {
     const val VIEW_MODE = 0
     const val CREATE_AND_EDIT_MODE = 1 // the default
    }

    val viewModel:  SubmissionViewModel
    val submission: Submission?
    val actionBarTitle: String?
    val mode: Int

    /**
     * Call this in the onResume() method
     */
    fun updateActionBarTitle()
}