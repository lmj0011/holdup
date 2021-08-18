package name.lmj0011.holdup.ui.submission

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentLinkSubmissionBinding
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.util.openUrlInWebBrowser
import org.kodein.di.instance
import java.net.URL


class LinkSubmissionFragment: Fragment(R.layout.fragment_link_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override val actionBarTitle: String = "Link Submission"
    override var mode: Int = SubmissionFragmentChild.CREATE_AND_EDIT_MODE

    private lateinit var binding: FragmentLinkSubmissionBinding
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper

    companion object {
        fun newInstance(submission: Submission?, mode: Int): LinkSubmissionFragment {
            val fragment = LinkSubmissionFragment()

            val args = Bundle().apply {
                putParcelable("submission", submission)
                putInt("mode", mode)
            }

            fragment.arguments = args

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()

        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        submission = requireArguments().getParcelable("submission") as? Submission
        mode = requireArguments().getInt("mode")
        setupBinding(view)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Link)
    }


    override fun updateActionBarTitle() {}

    override fun setupBinding(view: View) {
        binding = FragmentLinkSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        val url = submission?.url
        val linkImageUrl = submission?.linkImageUrl

        if (mode == SubmissionFragmentChild.VIEW_MODE) {
            binding.linkTextView.visibility = View.GONE
        } else {
            binding.linkTextView.setText(url)
        }

        if (linkImageUrl.isNullOrBlank() || url.isNullOrBlank()) {
            if (mode == SubmissionFragmentChild.CREATE_AND_EDIT_MODE) {
                binding.imageCard.visibility = View.GONE
            }
        } else {
            binding.linkCaptionTextView.text = URL(url).host

            binding.imageCard.setOnClickListener {
                openUrlInWebBrowser(requireContext(), url)
            }

            Glide
                .with(requireContext())
                .load(linkImageUrl)
                .into(binding.backgroundImageView)
        }
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