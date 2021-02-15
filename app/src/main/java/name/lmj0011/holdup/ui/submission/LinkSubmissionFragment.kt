package name.lmj0011.holdup.ui.submission

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentLinkSubmissionBinding
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.util.launchUI
import org.kodein.di.instance


class LinkSubmissionFragment(
    override var viewModel:  SubmissionViewModel,
    override val submission: Submission? = null,
    override val actionBarTitle: String? = "Link Submission"
): Fragment(R.layout.fragment_link_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    private lateinit var binding: FragmentLinkSubmissionBinding
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Link)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()

        submission?.let {
            binding.linkTextView.setText(it.url)
        }
    }

    override fun updateActionBarTitle() {
        actionBarTitle?.let {
            launchUI {
                (requireActivity() as AppCompatActivity).supportActionBar?.title = it
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentLinkSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
    }

    override fun setupObservers() {
        binding.linkTextView.addTextChangedListener( object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSubmissionLinkText(s.toString())
            }
        })


        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })
    }

    override fun setupRecyclerView() { }

    override fun clearUserInputViews() {
        binding.linkTextView.text.clear()
    }

}