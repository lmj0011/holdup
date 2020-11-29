package name.lmj0011.redditdraftking.ui.submission

import android.os.Bundle
import androidx.fragment.app.Fragment
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import org.kodein.di.instance

class ImageSubmissionFragment: Fragment(R.layout.fragment_image_submission) {
    private lateinit var  viewModel: SubmissionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.validateSubmission(SubmissionKind.Image)
    }
}